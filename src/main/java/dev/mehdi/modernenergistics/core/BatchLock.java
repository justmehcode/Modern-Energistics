package dev.mehdi.modernenergistics.core;

/** Persisted, server-thread-only job ledger. Finishing a recipe is not the same as delivering its output. */
public final class BatchLock {
    private String recipe = "";
    private long remaining;
    private boolean keep;

    public String recipe() { return recipe; }
    public long remaining() { return remaining; }
    public boolean keep() { return keep; }
    public boolean busy() { return remaining > 0; }
    public boolean permits(String candidate) { return busy() && recipe.equals(candidate); }
    public boolean canAccept(String candidate, boolean outputsEmpty) {
        return !busy() && outputsEmpty && (recipe.isEmpty() || !keep || recipe.equals(candidate));
    }
    public void accept(String candidate, long crafts, boolean outputsEmpty) {
        if (candidate.isEmpty() || crafts <= 0 || !canAccept(candidate, outputsEmpty))
            throw new IllegalStateException("Conflicting or invalid batch");
        recipe = candidate;
        remaining = crafts;
    }
    public void completeRecipe() {
        if (remaining > 0) remaining--;
    }
    public void refresh(boolean outputsEmpty) {
        if (!busy() && outputsEmpty && !keep) recipe = "";
    }
    public void setKeep(boolean keep) { this.keep = keep; }
    public void restore(String recipe, long remaining, boolean keep) {
        if (remaining < 0 || (remaining > 0 && recipe.isEmpty()))
            throw new IllegalArgumentException("Invalid saved batch");
        this.recipe = recipe;
        this.remaining = remaining;
        this.keep = keep;
    }
}
