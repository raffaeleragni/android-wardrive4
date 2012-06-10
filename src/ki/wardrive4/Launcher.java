package ki.wardrive4;

import android.app.Activity;
import android.os.Bundle;

/**
 * Launcher entry point.
 * 
 * Since activities mapped as LAUNCHER will be used for shortcuts, this one has
 * been created to maintain a common static entry point for the application to
 * be launched.
 * 
 * Also, global application initialization code goes here.
 * Example: checks for WiFi to be enabled.
 * 
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class Launcher extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }
}
