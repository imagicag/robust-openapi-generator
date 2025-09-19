// Copyright (C) 2025, Imagic Bildverarbeitung AG, Sägereistrasse 29, CH-8152 Glattbrugg
//
// This file will be replaced as part of the open api generation process DO NOT EDIT
//
// This file is provided under the following conditions:
// THE SOFTWARE IS PROVIDED “AS IS” AND THE AUTHOR DISCLAIMS ALL
// WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES
// OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE
// FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY
// DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN
// AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
// OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
//
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TimeoutBodyPublisher implements HttpRequest.BodyPublisher, Flow.Subscription {

    private final AtomicLong demand = new AtomicLong();
    private final InputStream inputStream;
    private volatile boolean cancelled;
    private volatile Flow.Subscriber<? super ByteBuffer> subscriber;
    private final Executor executor;
    private final Object mutex = new Object();
    private final long timeout;
    private final long totalTimeout;
    private final RequestContext context;

    public TimeoutBodyPublisher(Executor executor, InputStream inputStream, Duration timeout, Duration totalTimeout, RequestContext context) {
        this.inputStream = Objects.requireNonNull(inputStream);
        this.executor = Objects.requireNonNull(executor);
        this.context = Objects.requireNonNull(context);
        long tm;
        try {
            tm = timeout.toNanos();
        } catch (ArithmeticException ae) {
            tm = Long.MAX_VALUE;
        }

        if (tm < 0) {
            throw new IllegalArgumentException("timeout must be positive");
        }

        this.timeout = tm;

        try {
            tm = totalTimeout.toNanos();
        } catch (ArithmeticException ae) {
            tm = Long.MAX_VALUE;
        }
        if (tm < 0) {
            throw new IllegalArgumentException("totalTimeout must be positive");
        }

        this.totalTimeout = tm;
    }

    private void handlePull() {
        long totalStart = System.nanoTime();
        byte[] buffer = new byte[0x1_00_00];
        try(inputStream) {
            while(!cancelled) {
                int i = inputStream.read(buffer);
                if (i == 0) {
                    continue;
                }

                if (i < 0) {
                    if (!cancelled) {
                        context.onRequestBodyCompleted();
                        subscriber.onComplete();
                    }
                    return;
                }

                if (i > buffer.length) {
                    throw new IOException("input stream returned more data than the buffer it was given.");
                }

                if (cancelled) {
                    return;
                }

                ByteBuffer buf = ByteBuffer.allocate(i);
                buf.put(buffer, 0, i);
                buf.position(0);

                long start = System.nanoTime();
                while (this.demand.get() == 0) {

                    long now = System.nanoTime();
                    long left = totalTimeout - (now - totalStart);
                    if (left <= 0) {
                        throw new HttpTimeoutException("Total write timeout sending http request body");
                    }

                    synchronized (mutex) {
                        if (cancelled) {
                            return;
                        }
                        if (this.demand.get() == 0) {
                            long elapsed = now - start;
                            if (elapsed >= timeout) {
                                throw new HttpTimeoutException("Write timeout sending http request body");
                            }

                            long leftTimeout = timeout - elapsed;
                            mutex.wait(Math.max(1, TimeUnit.NANOSECONDS.toMillis(Math.min(leftTimeout, left))));
                        }
                    }
                }

                this.demand.getAndAdd(-1);
                if (!cancelled) {
                    subscriber.onNext(buf);
                }
            }
        } catch (Exception e) {
            if (!cancelled) {
                subscriber.onError(e);
                context.cancel(e);
            }
        }
    }

    @Override
    public long contentLength() {
        return -1;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        boolean alreadySubscribed = false;
        synchronized (mutex) {
            if (this.subscriber != null) {
                cancel();
                alreadySubscribed = true;
            } else {
                this.subscriber = subscriber;
            }
        }

        if (alreadySubscribed) {
            this.subscriber.onError(new IllegalStateException("subscribe called again with another subscriber"));
            throw new IllegalStateException("already subscribed");
        }

        try {
            subscriber.onSubscribe(this);
        } catch (Exception e) {
            cancel();
            throw e;
        }

        try {
            executor.execute(this::handlePull);
        } catch (Exception e) {
            cancel();
            subscriber.onError(e);
        }
    }

    @Override
    public void request(long n) {
        if (cancelled) {
            return;
        }

        if (n <= 0) {
            cancel();
            subscriber.onError(new IllegalArgumentException("non-positive subscription request: " + n));
            return;
        }

        demand.getAndAccumulate(n, (a, b) -> {
            //SATURATING ADDITION
            long v = a + b;
            if (v <= a) {
                return Long.MAX_VALUE;
            }
            return v;
        });

        synchronized (mutex) {
            mutex.notifyAll();
        }
    }

    @Override
    public void cancel() {
        cancelled = true;
        try {
            inputStream.close();
        } catch (Exception e) {
            //DONT CARE
        }

        synchronized (mutex) {
            mutex.notifyAll();
        }
    }
}
