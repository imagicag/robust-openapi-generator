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
import java.io.InterruptedIOException;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * BodySubscriber that acts like an InputStream, unlike the JDK provided implementation, this one supports timeouts.
 */
public class HttpResponseInputStream extends InputStream implements HttpResponse.BodySubscriber<InputStream> {
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap(new byte[0]);

    private static final AtomicReferenceFieldUpdater<HttpResponseInputStream, Flow.Subscription> SUBSCRIPTION_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(HttpResponseInputStream.class, Flow.Subscription.class, "subscription");

    private final BlockingQueue<List<ByteBuffer>> bufferQueue;

    private volatile Flow.Subscription subscription;
    private volatile boolean closed;
    private volatile boolean eof;
    private volatile Throwable error;
    private final Long responseBodyReadTimeout;
    private final Long responseBodyTotalTimeout;

    private final Object bufferMutex = new Object();
    private Iterator<ByteBuffer> currentIter;
    private ByteBuffer currentBuffer;
    private long timeSpentBlocking;

    public HttpResponseInputStream(Duration responseBodyReadTimeout, Duration responseBodyTotalTimeout) {
        this.responseBodyReadTimeout = responseBodyReadTimeout == null ? null : responseBodyReadTimeout.toMillis();
        this.responseBodyTotalTimeout = responseBodyTotalTimeout == null ? null : responseBodyTotalTimeout.toMillis();
        this.bufferQueue = new ArrayBlockingQueue<>(2);
        this.currentIter = Collections.emptyIterator();
        this.currentBuffer = EMPTY_BUFFER;
    }

    @Override
    public CompletionStage<InputStream> getBody() {
        return CompletableFuture.completedStage(this);
    }


    private ByteBuffer getBuffer() throws IOException {
        //Only one thread can be in here at any time, otherwise we could get into a deadlock
        synchronized (bufferMutex) {
            while (true) {
                if (currentBuffer.hasRemaining()) {
                    return currentBuffer;
                }

                if (!currentIter.hasNext()) {
                    List<ByteBuffer> buffas = bufferQueue.poll();
                    if (buffas != null) {
                        currentIter = buffas.iterator();

                        Flow.Subscription s = SUBSCRIPTION_UPDATER.get(this);
                        if (s != null) {
                            s.request(1);
                        }

                        continue;
                    }

                    if (closed) {
                        throw new IOException("closed", error);
                    }

                    if (eof) {
                        return null;
                    }

                    currentIter = pollQueue().iterator();

                    Flow.Subscription s = SUBSCRIPTION_UPDATER.get(this);
                    if (s != null) {
                        s.request(1);
                    }
                    continue;
                }

                ByteBuffer buffa = currentIter.next();
                if (buffa == null) {
                    continue;
                }
                currentBuffer = buffa;
            }
        }

    }

    private List<ByteBuffer> pollQueue() throws IOException {
        List<ByteBuffer> buffas;

        //Only one thread can be in here at any time, otherwise we could get into a deadlock
        if (responseBodyReadTimeout == null && responseBodyTotalTimeout == null) {
            //Infinite timeout, trivial case, simply block for ever
            try {
                return bufferQueue.take();
            } catch (InterruptedException ex) {
                throw new InterruptedIOException();
            }
        }
        long remainingTotal = Long.MAX_VALUE;
        if (responseBodyTotalTimeout != null) {
            remainingTotal = responseBodyTotalTimeout - timeSpentBlocking;
        }

        if (remainingTotal <= 0) {
            close();
            throw new HttpTimeoutException("exceeded total amount of time to wait on the network while reading response body");
        }

        long timeoutToUse = remainingTotal;
        if (responseBodyReadTimeout != null) {
            timeoutToUse = Math.min(timeoutToUse, responseBodyReadTimeout);
        }


        long start = System.currentTimeMillis();
        try {
            buffas = bufferQueue.poll(timeoutToUse, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new InterruptedIOException();
        } finally {
            long took = Math.max(0, System.currentTimeMillis()-start); //Clock may go backwards, cap it at 0.
            timeSpentBlocking += took;
        }

        if (buffas == null) {
            if (responseBodyReadTimeout == null || remainingTotal < responseBodyReadTimeout) {
                //This is it, no more retries will be made.
                close();
                throw new HttpTimeoutException("exceeded total amount of time to wait on the network while reading response body");
            }

            //User can retry this
            throw new HttpTimeoutException("read timeout while reading response body");
        }

        return buffas;
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        ByteBuffer buffer = getBuffer();
        if (buffer == null) {
            return -1;
        }

        if (len == 0) {
            return 0;
        }

        int read = Math.min(buffer.remaining(), len);

        buffer.get(bytes, off, read);
        return read;
    }

    @Override
    public int read() throws IOException {
        ByteBuffer buffer = getBuffer();
        if (buffer == null) {
            return -1;
        }
        return buffer.get() & 0xFF;
    }

    @Override
    public void onSubscribe(Flow.Subscription s) {
        Objects.requireNonNull(s);
        if (closed || eof || !SUBSCRIPTION_UPDATER.compareAndSet(this, null, s)) {
            s.cancel();
            return;
        }

        s.request(1);
    }

    @Override
    public void onNext(List<ByteBuffer> t) {
        if (eof || closed) {
            return;
        }

        if (t == null) {
            onError(new NullPointerException("onNext: Received null item"));
            return;
        }

        if (!bufferQueue.offer(t)) {
            onError(new IllegalStateException("onNext: bufferQueue full"));
        }
    }

    @Override
    public void onError(Throwable error) {
        this.error = error;
        closed = true;

        Flow.Subscription s = SUBSCRIPTION_UPDATER.getAndSet(this, null);

        if (s != null) {
            s.cancel();
        }

        bufferQueue.offer(Collections.emptyList());
    }

    @Override
    public void onComplete() {
        eof = true;
        SUBSCRIPTION_UPDATER.set(this, null);
        bufferQueue.offer(Collections.emptyList());
    }

    @Override
    public void close() throws IOException {
        closed = true;

        Flow.Subscription s = SUBSCRIPTION_UPDATER.getAndSet(this, null);

        if (s != null) {
            s.cancel();
        }

        bufferQueue.offer(Collections.emptyList());
    }

}
