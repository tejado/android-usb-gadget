package net.tjado.usbgadget;

import android.app.Activity;
import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.List;

public class GadgetAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Activity context;
    private LayoutInflater inflater;
    private final GadgetAdapterClickInterface listener;
    List<GadgetObject> data;

    public GadgetAdapter(Activity context, List<GadgetObject> data, GadgetAdapterClickInterface clickListener) {
        this.context = context;
        this.data = data;
        listener = clickListener;
    }

    // Inflate the layout when viewholder created
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = inflater.from(parent.getContext()).
                inflate(R.layout.cardview_gadget, parent, false);
        return new DefaultViewHolder(view, listener);
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

        holder.status.setChecked(holder.gadget.isActivated());

        String udc = holder.gadget.getValue("udc");
        holder.udc.setText(udc);
        if (holder.gadget.isActivated()) {
            holder.card.setCardBackgroundColor(Color.WHITE);
        } else {
            holder.card.setCardBackgroundColor(Color.LTGRAY);
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
        Switch status;

        private WeakReference<GadgetAdapterClickInterface> listenerRef;

        public DefaultViewHolder(View itemView, GadgetAdapterClickInterface clickInterface) {
            super(itemView);
            card = itemView.findViewById(R.id.cv_gadget);
            manufacturer = itemView.findViewById(R.id.tv_gadget_manufacturer);
            product = itemView.findViewById(R.id.tv_gadget_product);
            serialnumber = itemView.findViewById(R.id.tv_gadget_sn);
            udc = itemView.findViewById(R.id.tv_gadget_udc);
            path = itemView.findViewById(R.id.tv_gadget_path);
            functions = itemView.findViewById(R.id.tv_gadget_functions);
            status = itemView.findViewById(R.id.tv_gadget_status);

            status.setOnClickListener((buttonView) -> {
                listenerRef.get().onStatusChange(gadget, status.isChecked());
            });

            listenerRef = new WeakReference<>(clickInterface);

            itemView.setOnClickListener(view -> {
                listenerRef.get().onGadgetClicked( gadget );
            });
        }
    }

}