package valerino.vgcamera;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by valerino on 13/09/15.
 */
public class Utils {
    /**
     * attempt to close a closeable object handling exception eventually
     * @param c a Closeable (OutputStream, ...)
     */
    public static void closeNoEx(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                // swallow
            }
        }
    }
}
