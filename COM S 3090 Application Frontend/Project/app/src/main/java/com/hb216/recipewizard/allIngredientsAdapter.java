package com.hb216.recipewizard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Adapter updates recycler view when displayed items change
 *
 * @author Maddie Sells
 */
public class allIngredientsAdapter extends RecyclerView.Adapter<allIngredientsAdapter.holder> implements recyclerInterface_MS{
        List<JSONObject> data;
        private final recyclerInterface_MS recyclerInterface;


        /**
         * Contructer for rvAdapter
         *
         * @param data arrayList of given recipe/profile data
         */
        public allIngredientsAdapter(List<JSONObject> data, recyclerInterface_MS recyclerInterface) {
            this.data = data;
            this.recyclerInterface = recyclerInterface;
        }

        /**
         * Sets search view list based on given filteredList
         * @param filteredList list of objects to change data to
         */
        void filterList(List<JSONObject> filteredList) {
            this.data = filteredList;
            notifyDataSetChanged();
        }

        /**
         * @param parent   The ViewGroup into which the new View will be added after it is bound to
         *                 an adapter position.
         * @param viewType The view type of the new View.
         * @return holder
         */
        @NonNull
        @Override
        public allIngredientsAdapter.holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.single_ingredient_template, parent, false);
            return new allIngredientsAdapter.holder(view);
        }

        /**
         * sets the title and image for each item in each recycler view
         *
         * @param holder   The ViewHolder which should be updated to represent the contents of the
         *                 item at the given position in the data set.
         * @param position The position of the item within the adapter's data set.
         */
        @Override
        public void onBindViewHolder(@NonNull allIngredientsAdapter.holder holder, int position) {
            try {
                //setting the title
                String ingName = data.get(position).getString("ingredientName");
                holder.ingTitle.setText(ingName);

            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * @return Size of dataset
         */
        @Override
        public int getItemCount() {
            return data.size();
        }

        /**
         * Sets search view list based on given filteredList
         *
         * @param filteredList list of objects to change data to
         */
        public void setFilteredList(List<JSONObject> filteredList) {
            this.data = filteredList;
            notifyDataSetChanged();
        }

    @Override
    public void onItemClick(int position, int whatDataset) throws JSONException {

    }

    /**
         * Class for the single recycler view templates
         *
         * @author Maddie Sells
         */
        class holder extends RecyclerView.ViewHolder {
            TextView ingTitle;

            /**
             * Constructor for holder class
             *
             * @param itemView given itemView
             */
            public holder(@NonNull View itemView) {
                super(itemView);
                ingTitle = itemView.findViewById(R.id.ingTitle);

                //Calls onItem click method for given position in given arrayList
                itemView.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        if(recyclerInterface != null){
                            int pos = getAdapterPosition();

                            if(pos != RecyclerView.NO_POSITION){
                                try {
                                    recyclerInterface.onItemClick(pos, 0);
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                });
            }

        }
}
