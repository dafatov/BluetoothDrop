package ru.demetrious.bluetoothdrop.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import ru.demetrious.bluetoothdrop.R;
import ru.demetrious.bluetoothdrop.activities.MainActivity;

public class FriendsElementAdapter extends ArrayAdapter<FriendsElement> {
    private LayoutInflater inflater;
    private int layout;
    private ArrayList<FriendsElement> friendsElements;
    private MainActivity mainActivity;

    public FriendsElementAdapter(MainActivity mainActivity, int resource, ArrayList<FriendsElement> friendsElements) {
        super(mainActivity, resource, friendsElements);
        this.mainActivity = mainActivity;
        this.friendsElements = friendsElements;
        this.layout = resource;
        this.inflater = LayoutInflater.from(mainActivity);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;

        if (convertView == null) {
            convertView = inflater.inflate(this.layout, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final FriendsElement friendsElement = friendsElements.get(position);

        viewHolder.name.setText(friendsElement.getBluetoothDevice().getName());
        viewHolder.address.setText(friendsElement.getBluetoothDevice().getAddress());

        if (friendsElement.isOnline()) {
            if (mainActivity.getBluetooth().getDevice() != null && friendsElement.getBluetoothDevice().getAddress().equals(mainActivity.getBluetooth().getDevice().getAddress()))
                viewHolder.status.setImageResource(R.drawable.ic_action_bluetooth_connected);
            else
                viewHolder.status.setImageResource(R.drawable.ic_action_bluetooth_online);
        } else {
            viewHolder.status.setImageResource(R.drawable.ic_action_bluetooth_offline);
        }
        return convertView;
    }

    private class ViewHolder {
        final TextView name, address;
        final ImageView status;

        ViewHolder(View view) {
            name = view.findViewById(R.id.friends_name);
            address = view.findViewById(R.id.friends_address);
            status = view.findViewById(R.id.friends_status);
        }
    }
}
