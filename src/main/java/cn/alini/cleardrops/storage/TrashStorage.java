package cn.alini.cleardrops.storage;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public class TrashStorage extends SimpleContainer {

    public TrashStorage(int size) {
        super(size);
    }

    public ItemStack insertStack(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack toInsert = stack.copy();

        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack cur = this.getItem(i);
            if (!cur.isEmpty() && ItemStack.isSameItemSameTags(cur, toInsert)) {
                int canMove = Math.min(cur.getMaxStackSize() - cur.getCount(), toInsert.getCount());
                if (canMove > 0) {
                    cur.grow(canMove);
                    toInsert.shrink(canMove);
                    this.setChanged();
                    if (toInsert.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack cur = this.getItem(i);
            if (cur.isEmpty()) {
                int move = Math.min(toInsert.getCount(), toInsert.getMaxStackSize());
                ItemStack put = toInsert.copy();
                put.setCount(move);
                this.setItem(i, put);
                toInsert.shrink(move);
                this.setChanged();
                if (toInsert.isEmpty()) return ItemStack.EMPTY;
            }
        }
        return toInsert;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return false;
    }
}