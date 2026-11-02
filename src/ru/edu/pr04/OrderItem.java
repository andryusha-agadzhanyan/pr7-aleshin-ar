package ru.edu.pr04;

public class OrderItem {
    public static final int MAX_QUANTITY = 100;

    private final Product product;
    private int quantity;

    public OrderItem(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException("product is null");
        }
        this.product = product;
        setQuantity(quantity);
    }

    private void setQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("quantity must be in [1, " + MAX_QUANTITY + "]");
        }
        this.quantity = quantity;
    }

    public void increase(int delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException("delta must be > 0");
        }
        // Проверка до сложения защищает и от переполнения int.
        if (delta > MAX_QUANTITY - quantity) {
            throw new IllegalArgumentException("quantity must be in [1, " + MAX_QUANTITY + "]");
        }
        setQuantity(quantity + delta);
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }

    public double getLineTotal() {
        return product.getPrice() * quantity;
    }
}
