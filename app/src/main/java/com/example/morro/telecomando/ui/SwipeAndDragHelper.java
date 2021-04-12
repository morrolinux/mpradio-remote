package com.example.morro.telecomando.ui;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.support.v7.widget.helper.ItemTouchHelper.Callback;

/**
 * Created by morro on 17/02/18.
 */

/** Handle the gestures by calling adapter's ActionCompletionContract methods */

public class SwipeAndDragHelper extends Callback {

    /** declare the interface */
    private ActionCompletionContract contract;

    /**
     * the passed object implements the param.'s requested interface
     * we could have had a simple ItemAdapter object instance, but this way we're keeping it
     * as generic as possible.
     */
    public SwipeAndDragHelper(ActionCompletionContract contract) {
        this.contract = contract;
    }

    /** Define flags for IDLE, DRAG and SWIPE */
    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    /** gets called when a view is dragged from its position to other positions */
    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        //contract.onViewMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    /** let's disable item dragging since we won't need it. */
    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    /** gets called when a view is completely swiped out */
    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        contract.onViewSwiped(viewHolder.getAdapterPosition(), direction);
    }

    public interface ActionCompletionContract {
        void onViewMoved(int oldPosition, int newPosition);
        void onViewSwiped(int position, int direction);
    }
}
