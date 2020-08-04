package net.tjado.usbgadget;

import android.app.Activity;
import android.graphics.Color;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

public class GadgetAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Activity context;
    private LayoutInflater inflater;
    List<GadgetObject> data = Collections.emptyList();

    public GadgetAdapter(Activity context, List<GadgetObject> data) {
        this.context = context;
        this.data = data;
    }

    // Inflate the layout when viewholder created
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = inflater.from(parent.getContext()).
                inflate(R.layout.cardview_gadget, parent, false);
        return new DefaultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        initLayoutDefault((DefaultViewHolder) holder, position);
    }


    @Override
    public int getItemCount() {
        return data.size();
    }

    private void initLayoutDefault(DefaultViewHolder holder, int pos) {
        holder.gadget = data.get(pos);
        holder.manufacturer.setText(holder.gadget.getValue("manufacturer"));
        holder.product.setText(holder.gadget.getValue("product"));
        holder.serialnumber.setText(holder.gadget.getValue("serialnumber"));
        holder.path.setText(holder.gadget.getValue("gadget_path"));
        holder.functions.setText(Html.fromHtml(holder.gadget.getFormattedFunctions(), Html.FROM_HTML_MODE_LEGACY));

        String udc = holder.gadget.getValue("udc");
        holder.udc.setText(udc);
        if (holder.gadget.isActivated()) {
            holder.card.setCardBackgroundColor(Color.GREEN);
            holder.activate.setText(R.string.deactivate);
        } else {
            holder.activate.setText(R.string.activate);
            holder.card.setCardBackgroundColor(Color.WHITE);
        }

    }

    // Static inner class to initialize the views of rows
    static class DefaultViewHolder extends RecyclerView.ViewHolder {
        GadgetObject gadget;
        CardView card;
        TextView manufacturer;
        TextView product;
        TextView serialnumber;
        TextView udc;
        TextView functions;
        TextView path;
        Button activate;

        public DefaultViewHolder(View itemView) {
            super(itemView);
            card = (CardView) itemView.findViewById(R.id.cv_gadget);
            manufacturer = (TextView) itemView.findViewById(R.id.tv_gadget_manufacturer);
            product = (TextView) itemView.findViewById(R.id.tv_gadget_product);
            serialnumber = (TextView) itemView.findViewById(R.id.tv_gadget_sn);
            udc = (TextView) itemView.findViewById(R.id.tv_gadget_udc);
            path = (TextView) itemView.findViewById(R.id.tv_gadget_path);
            functions = (TextView) itemView.findViewById(R.id.tv_gadget_functions);
            activate = (Button) itemView.findViewById(R.id.activate);

            activate.setOnClickListener(view -> {

                String gadgetPath = gadget.getValue("gadget_path");
                String[] commands = {};

                if (gadgetPath == null || !gadgetPath.startsWith("/config/")) {
                    Log.d("root", String.format("gadgetPath errornous value: %s", gadgetPath));
                    return;
                }

                if (gadget.isActivated()) {
                    // deactivate specific gadget
                    String cmdDeactivateUsb = String.format("echo \"\" > %s/UDC\n", gadgetPath);

                    commands = new String[]{cmdDeactivateUsb};
                } else {
                    // deactivate all
                    String cmdDeactivateUsbAll = "find /config/usb_gadget/  -name UDC -type f -exec sh -c 'echo \"\" >  \"$@\"' _ {} \\;\n";
                    // activate clicked one
                    String cmdActivateUsb = String.format("getprop sys.usb.controller > %s/UDC\n", gadgetPath);

                    commands = new String[]{cmdDeactivateUsbAll, cmdActivateUsb};
                }

                RootTask mTask = new RootTask(commands, response -> {
                    //mTextView.setText((String) response);
                });
                mTask.execute();

                ((MainActivity) itemView.getContext()).refreshGadgets();
                return;
            });
        }

    }

}