/**
 * Authorizer
 *
 *  Copyright 2016 by Tjado Mäcke <tjado@maecke.de>
 *  Licensed under GNU General Public License 3.0.
 *
 * @license GPL-3.0 <https://opensource.org/licenses/GPL-3.0>
 */
package net.tjado.usbgadget;

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
                    Log.d("ROOT", String.format("Execute command: %s", cmd));
                    stdin.writeBytes(cmd);
                    stdin.flush();
                }

                stdin.writeBytes("exit\n");
                stdin.flush();

                stdin.close();

                StringBuffer sbOut = new StringBuffer();
                Scanner scanner = new Scanner(stdout);
                while (scanner.hasNext()) {
                    sbOut.append(scanner.nextLine());
                    sbOut.append(System.lineSeparator());
                }

                StringBuffer sbErr = new StringBuffer();
                Scanner scannerErr = new Scanner(stderr);
                while (scannerErr.hasNext()) {
                    sbErr.append(scannerErr.nextLine());
                    if (scannerErr.hasNext())
                        sbErr.append(System.lineSeparator());
                }

                output = sbOut.toString();
                Log.d("ROOT (stdout)", output);

                String error = sbErr.toString();
                Log.d("ROOT (stderr)", error);

                retval = ! error.equals("Permission denied");
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