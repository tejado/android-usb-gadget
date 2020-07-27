/**
 * Authorizer
 *
 *  Copyright 2016 by Tjado Mäcke <tjado@maecke.de>
 *  Licensed under GNU General Public License 3.0.
 *
 * @license GPL-3.0 <https://opensource.org/licenses/GPL-3.0>
 */
package net.tjado.usbgadget;

import android.util.Log;
import android.util.Pair;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/*
 * Class by muzikant <http://stackoverflow.com/users/624109/muzikant>
 *
 * Reference:
 * http://muzikant-android.blogspot.com/2011/02/how-to-get-root-access-and-execute.html
 * http://stackoverflow.com/a/7102780
 */

public class ExecuteAsRootUtil
{

    public static boolean canRunRootCommands()
    {
        boolean retval = false;
        Process suProcess;

        try
        {
            suProcess = Runtime.getRuntime().exec("su");

            DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
            DataInputStream osRes = new DataInputStream(suProcess.getInputStream());

            if (null != os && null != osRes)
            {
                // Getting the id of the current user to check if this is root
                os.writeBytes("id\n");
                os.flush();

                String currUid = osRes.readLine();
                boolean exitSu = false;
                if (null == currUid)
                {
                    retval = false;
                    exitSu = false;
                    Log.d("ROOT", "Can't get root access or denied by user");
                }
                else if (true == currUid.contains("uid=0"))
                {
                    retval = true;
                    exitSu = true;
                    Log.d("ROOT", "Root access granted");
                }
                else
                {
                    retval = false;
                    exitSu = true;
                    Log.d("ROOT", "Root access rejected: " + currUid);
                }

                if (exitSu)
                {
                    os.writeBytes("exit\n");
                    os.flush();
                }
            }
        }
        catch (Exception e)
        {
            // Can't get root !
            // Probably broken pipe exception on trying to write to output stream (os) after su failed, meaning that the device is not rooted

            retval = false;
            Log.d("ROOT", "Root access rejected [" + e.getClass().getName() + "] : " + e.getMessage());
        }

        return retval;
    }

    public static final Pair<Boolean, String> execute(String command) {
        return execute(new String[]{ command });
    }

    public static final Pair<Boolean, String> execute(String[] commands)
    {
        Boolean retval = false;
        String output = null;

        try
        {
            if (commands != null && commands.length > 0)
            {
                Process suProcess = Runtime.getRuntime().exec("su -");

                DataOutputStream stdin = new DataOutputStream(suProcess.getOutputStream());
                InputStream stdout = suProcess.getInputStream();
                InputStream stderr = suProcess.getErrorStream();

                for (String cmd: commands) {
                    Log.d("root", String.format("Execute command: %s", cmd));
                    stdin.writeBytes(cmd);
                    stdin.flush();
                }

                stdin.writeBytes("exit\n");
                stdin.flush();

                stdin.close();

                StringBuffer sb = new StringBuffer();
                Scanner scanner = new Scanner(stdout);
                while (scanner.hasNext()) {
                    sb.append(scanner.nextLine());
                    sb.append(System.lineSeparator());
                }

                Scanner scannerErr = new Scanner(stderr);
                while (scannerErr.hasNext()) {
                    Log.d("root", scannerErr.nextLine());
                }

                output = sb.toString();
                Log.d("root", output);

                try
                {
                    int suProcessRetval = suProcess.waitFor();
                    if (255 != suProcessRetval)
                    {
                        // Root access granted
                        retval = true;
                    }
                    else
                    {
                        // Root access denied
                        retval = false;
                    }
                }
                catch (Exception ex)
                {
                    Log.e("ROOT", "Error executing root action", ex);
                }
            }
        }
        catch (IOException ex)
        {
            Log.w("ROOT", "Can't get root access", ex);
        }
        catch (SecurityException ex)
        {
            Log.w("ROOT", "Can't get root access", ex);
        }
        catch (Exception ex)
        {
            Log.w("ROOT", "Error executing internal operation", ex);
        }

        return new Pair(retval, output);
    }


    public static final Pair<Boolean, byte[]> executeBinaryOut(String[] commands)
    {
        Boolean retval = false;
        byte[] output = new byte[64];;

        try
        {
            if (commands != null && commands.length > 0)
            {
                int count = 64;
                Process suProcess = Runtime.getRuntime().exec("su -");

                DataOutputStream stdin = new DataOutputStream(suProcess.getOutputStream());
                InputStream stdout = suProcess.getInputStream();
                InputStream stderr = suProcess.getErrorStream();

                for (String cmd: commands) {
                    Log.d("root", String.format("Execute command: %s", cmd));
                    stdin.writeBytes(cmd);
                    stdin.flush();
                }

                stdin.writeBytes("exit\n");
                stdin.flush();
                stdin.close();

                int tmp = stdout.read(output, 0, 64);
                Log.d("muh", String.format("read %d of data", tmp));
                stdout.close();

                Scanner scannerErr = new Scanner(stderr);
                while (scannerErr.hasNext()) {
                    Log.d("root", scannerErr.nextLine());
                }

                try
                {
                    int suProcessRetval = suProcess.waitFor();
                    if (255 != suProcessRetval)
                    {
                        // Root access granted
                        retval = true;
                    }
                    else
                    {
                        // Root access denied
                        retval = false;
                    }
                }
                catch (Exception ex)
                {
                    Log.e("ROOT", "Error executing root action", ex);
                }
            }
        }
        catch (IOException ex)
        {
            Log.w("ROOT", "Can't get root access", ex);
        }
        catch (SecurityException ex)
        {
            Log.w("ROOT", "Can't get root access", ex);
        }
        catch (Exception ex)
        {
            Log.w("ROOT", "Error executing internal operation", ex);
        }

        return new Pair(retval, output);
    }

}