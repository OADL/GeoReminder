package cse.oadl.geo_alarm;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import static android.content.ContentValues.TAG;

public class ReminderAdapter extends ArrayAdapter<Reminder> {

    private static class ViewHolder {
        TextView name;
        TextView address;
        Switch onoff;
        ImageButton delete;
    }

    private LayoutInflater inflater;
    private ArrayList<Reminder> reminders;
    private AppCompatActivity activity;

    public ReminderAdapter(Context context, int resource, ArrayList<Reminder> items,AppCompatActivity activity){
        super(context,resource, items);
        inflater = LayoutInflater.from(context);
        this.reminders = items;
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return reminders.size();
    }

    @Override
    public Reminder getItem(int position) {
        return reminders.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public AppCompatActivity getActivity() {
        return activity;
    }

    public void setActivity(AppCompatActivity activity) {
        this.activity = activity;
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        final Reminder reminder = getItem(position);
        ViewHolder vh;
        //=====================================

        if (view == null) {
            vh = new ViewHolder();
            inflater = LayoutInflater.from(getContext());
            //=====================================
            view = inflater.inflate(R.layout.reminderlayout, parent, false);
            //=====================================
            vh.name = (TextView) view.findViewById(R.id.nametext);
            vh.address = (TextView) view.findViewById(R.id.addresstext);
            vh.onoff = (Switch) view.findViewById(R.id.onoff);
            vh.delete = (ImageButton) view.findViewById(R.id.deletebutton);
            view.setTag(vh);

        }
        //=====================================


        else {
            vh = (ViewHolder) view.getTag();
        }
        //=====================================

        vh.name.setText(reminder.getName());
        vh.address.setText(reminder.getAddress());
        vh.onoff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Log.d(TAG, "onCheckedChanged: position: "+position);
                reminders.get(position).setEnabled(b);
                try {
                    updateReminder(position);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        vh.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int oldsize = reminders.size();
                reminders.remove(position);
                for(int  i = 0 ; i < oldsize ; i++){
                    getContext().deleteFile("reminder"+i+".txt");
                    if(i < oldsize - 1){
                        try {
                            updateReminder(i);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    updatereminderssize();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                getActivity().recreate();
            }
        });
        vh.onoff.setChecked(reminder.getEnabled());
        return view;
    }

    private void updateReminder(int i) throws IOException{
        Log.d(TAG, "updateReminder: reminder"+i+".txt");
        FileOutputStream fos = getContext().openFileOutput("reminder"+i+".txt", Context.MODE_PRIVATE);
        ObjectOutputStream os = new ObjectOutputStream(fos);
        os.writeObject(getItem(i));
        os.close();
        fos.close();
    }

    private void updatereminderssize() throws IOException{
        Log.d(TAG, "updatereminderssize: Reminders size : " + reminders.size());
        FileOutputStream fos = getContext().openFileOutput("reminders.txt", Context.MODE_PRIVATE);
        ObjectOutputStream os = new ObjectOutputStream(fos);
        os.writeInt(reminders.size());
        os.close();
        fos.close();
    }
}
