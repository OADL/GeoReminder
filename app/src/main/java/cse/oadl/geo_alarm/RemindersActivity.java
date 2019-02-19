package cse.oadl.geo_alarm;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class RemindersActivity extends AppCompatActivity {

    private static final String TAG = "ok";
    private ListView listView;

    private ArrayList<Reminder> reminders;
    private ReminderAdapter reminderAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);
        reminders = new ArrayList<>();

        try {
            getReminders();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        listView = (ListView) findViewById(R.id.listview);
        reminderAdapter = new ReminderAdapter(this, R.layout.reminderlayout,reminders,this);
        listView.setAdapter(reminderAdapter);

    }

    private void getReminders() throws IOException,ClassNotFoundException{
        FileInputStream fis = this.openFileInput("reminders.txt");
        ObjectInputStream is = new ObjectInputStream(fis);
        int i = -1;
        if(fis != null){
            i = is.readInt();
            Log.d(TAG, "getReminders: size: "+i);
            is.close();
            fis.close();
            for(int l = 0 ; l < i ; l++){
                fis = this.openFileInput("reminder"+l+".txt");
                is = new ObjectInputStream(fis);
                Reminder r;
                if((r = (Reminder) is.readObject()) != null){
                    reminders.add(r);
                }
                is.close();
                fis.close();
            }
        }else{
            is.close();
            fis.close();
        }
    }

}
