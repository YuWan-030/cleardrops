package cn.alini.cleardrops.storage;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public class TrashStorage extends SimpleContainer {

    public TrashStorage(int size) {
        super(size);
    }

    // 合并插入，返回未能插入的剩余
    public ItemStack insertStack(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack toInsert = stack.copy();

        // 先合并同类物品
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
        // 再放空位
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
        // 禁止玩家向回收站放入
        return false;
    }
}