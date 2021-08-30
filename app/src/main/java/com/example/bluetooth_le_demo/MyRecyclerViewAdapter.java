package com.example.bluetooth_le_demo;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.MyViewHolder> {

    private ArrayList<BluetoothDevice> devices;
    private Context context;

    public MyRecyclerViewAdapter(Context context, ArrayList<BluetoothDevice> devices){
        this.devices = devices;
        this.context = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.my_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        String deviceName = devices.get(position).getName();
        if(deviceName != null && deviceName.length() > 0)
            holder.deviceNameTxt.setText(deviceName);
        else
            holder.deviceNameTxt.setText("Unknown Device");
        holder.deviceAddressTxt.setText(devices.get(position).getAddress());

        holder.rowLayout.setOnClickListener(v -> {
            Intent intent = new Intent(context, DeviceControlActivity.class);
            intent.putExtra("device", devices.get(position));
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView deviceNameTxt;
        TextView deviceAddressTxt;
        ConstraintLayout rowLayout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceNameTxt = itemView.findViewById(R.id.deviceName);
            deviceAddressTxt = itemView.findViewById(R.id.deviceAddress);
            rowLayout = itemView.findViewById(R.id.rowLayout);
        }
    }
}
