package net.holm.boosternoti;

public class SaleSummaryManager {
    private static SaleSummaryManager instance;
    private long totalSales;  // Keep track of the total sales directly

    // Singleton instance
    public static SaleSummaryManager getInstance() {
        if (instance == null) {
            instance = new SaleSummaryManager();
        }
        return instance;
    }

    SaleSummaryManager() {
        this.totalSales = 0;  // Initialize the total sales as zero
    }

    // Add a sale amount to the running total
    public void addSale(long saleAmount) {
        totalSales += saleAmount;
    }

    // Get the current total sales
    public long getTotalSales() {
        return totalSales;
    }

    // Clear the total sales (reset to zero)
    public void clearSales() {
        totalSales = 0;
    }
}