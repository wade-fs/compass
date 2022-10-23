package com.wade.compass;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.wade.libs.CPDB;
import com.wade.libs.Tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CpsAdapter extends ArrayAdapter<CPDB.CP> {
    Context context;
    private ArrayList<CPDB.CP> dataSet;

    public CpsAdapter(Context context, ArrayList<CPDB.CP> cps) {
        super(context, R.layout.cps_list, cps);
        this.context = context;
        dataSet = cps;
    }

    private static class ViewHolder {
        TextView cpStr;
    }

    private int lastPosition = -1;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        CPDB.CP cp = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.cps_list, parent, false);
            viewHolder.cpStr = (TextView) convertView.findViewById(R.id.liCp);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        lastPosition = position;
        viewHolder.cpStr.setText(String.format(Locale.CHINESE, "[%s]%s#%d E%.0f,N%.0f\n",
                cp.number, (cp.name.length()>0?cp.name:""), cp.t,
                cp.x, cp.y));
        return convertView;
    }
}
