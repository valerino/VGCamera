package valerino.vgcamera;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * generic utilities
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

    /**
     * creates a new file out of a buffer
     * @param buf the data to be written
     * @param path path to the file to be created (will be overwritten)
     * @return a File (pointing at path), or null on error
     */
    public static File bufferToFile (byte[] buf, final String path) {
        FileOutputStream fos = null;
        File f = new File (path);
        try {
            fos = new FileOutputStream(f);
            fos.write(buf);
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            f.delete();
            return null;
        }
        finally {
            closeNoEx(fos);
        }
        return f;
    }

    /**
     * read input stream to a byte array
     * @param inputStream the input stream
     * @param maxRead max bytes to be read, 0 to read all input stream
     * @return byte[]
     * @throws IOException
     */
    public static byte[] inputStreamToByteArray (InputStream inputStream, int maxRead) throws IOException {
        byte[] buf = new byte[8192];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        int totRead = 0;
        while (true) {
            int read = 0;
            try {
                read = inputStream.read(buf);
            }
            finally {
                bos.close();
            }
            if (read == -1) {
                break;
            }
            if (maxRead == 0) {
                // write all
                bos.write(buf,0,read);
            }
            else {
                if (totRead < maxRead) {
                    // write a full chunk
                    bos.write(buf,0,read);
                }
                else {
                    // last chunk
                    bos.write(buf,0,maxRead - totRead);
                    bos.close();
                    break;
                }
            }
            totRead += read;
        }
        return bos.toByteArray();
    }

    /**
     * read a file to buffer
     * @param f file to be read
     * @return a byte array or null
     */
    public static byte[] fileToBuffer(File f) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            return null;
        }
        byte[] buf = null;
        try {
            buf = inputStreamToByteArray(fis, 0);
        } catch (IOException e) {
            return null;
        }
        finally {
            closeNoEx(fis);
        }
        return buf;
    }

    /**
     * creates a temporary file out of a buffer
     * @param buf the data to be written
     * @return a File (should be deleted after usage), or null on error
     */
    public static File bufferToTempFile(byte[] buf) {
        File tmpFile;
        try {
            tmpFile = File.createTempFile(null,null);
        } catch (IOException e) {
            return null;
        }
        File f = bufferToFile(buf, tmpFile.getAbsolutePath());
        return f;
    }

    /**
     * create a private app file out of a buffer (uses openFileOutput())
     * @param ctx a Context
     * @param buf the data to be written
     * @param filename name of the file
     * @return File or null
     */
    public static File bufferToPrivateFile(Context ctx, byte[] buf, final String filename) {
        // this is the file we will have as output
        File f = new File (ctx.getFilesDir(), filename);

        // create the private file
        FileOutputStream fos = null;
        try {
            fos = ctx.openFileOutput(filename, 0);
        } catch (FileNotFoundException e) {
            return null;
        }

        // and write to it
        try {
            fos.write(buf);
        } catch (IOException e) {
            f.delete();
            return null;
        }
        finally {
            closeNoEx(fos);
        }

        // done
        return f;
    }

}
