package com.hb216.recipewizard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Used to dynamically display a list of usernames that the user can click on.
 *
 * @author Jeffrey Remy
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    /** A list of usernames that the user has either messaged or been messaged by. */
    private List<String> storedNames;

    /**
     * Initializes the storedNames of ChatAdapter.
     * @param names a list of username Strings
     */
    public ChatAdapter(List<String> names) {
        storedNames = names;
    }

    /**
     * An interface that can be implemented in the chat activity so that item clicks
     * in ViewHolder are received in ChatActivity.
     */
    public interface OnItemClickListener {
        void onItemClick(View itemView, int position);
    }

    /** A listener for clicks on individual usernames in the ChatAdapter.*/
    private OnItemClickListener listener;

    /**
     * Sets the OnItemClickListener of ChatAdapter so that individual usernames can be clicked on.
     * @param listener the OnItemClickListener that carries out actions in response to an item being clicked
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Displays the UI elements for individual usernames and handles clicking on individual items.
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        // variable for displaying username in the list
        public TextView tvName;


        // custom viewholder constructor
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvChatMenuName);

            // set click listener for individual usernames
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // send click to be received by interface
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            // pass in info about where the user clicked
                            listener.onItemClick(itemView, position);
                        }
                    }
                }
            });
        }
    }

    /**
     * Adds a row to the chat menu by inflating the display.
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     *               an adapter position.
     * @param viewType The view type of the new View.
     *
     * @return the new ViewHolder that will display a username in the chat menu
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // insert new chat menu row to the recycler view
        View chatMenuView = inflater.inflate(R.layout.chatmenu_row, parent, false);

        ViewHolder viewHolder = new ViewHolder(chatMenuView);
        return viewHolder;
    }

    /**
     * Populates data in a newly created chat menu row, such as displaying the username.
     * @param holder The ViewHolder which should be updated to represent the contents of the
     *        item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // get corresponding name based on position in list
        String name = storedNames.get(position);

        // set TextView
        TextView textView = holder.tvName;
        textView.setText(name);
    }

    /**
     * Returns the number of stored usernames.
     * @return the number of usernames in storedNames
     */
    @Override
    public int getItemCount() {
        return storedNames.size();
    }

}
