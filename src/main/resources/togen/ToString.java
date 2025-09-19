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
public interface ToString {

    /**
     * ToString implementation the prefixes each new line with the given level of indents.
     * Calling this with parameter 0 is equivalent to calling {@link Object#toString()}.
     */
    String toString(int level);

    /**
     * Utility that either invokes the toString(level) method
     * or calls String.valueOf(obj) if obj is not an instance of ToString.
     */
    static String toString(Object obj, int level) {
        if (obj instanceof ToString) {
            return ((ToString) obj).toString(level);
        }
        return String.valueOf(obj);
    }
}
