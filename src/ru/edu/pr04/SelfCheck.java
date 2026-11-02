package ru.edu.pr04;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class SelfCheck {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        try {
            if (args.length == 1 && args[0].equals("--interactive")) {
                runInteractive();
                return;
            }
            Path file = Path.of("data/sample.csv");
            if (args.length == 2 && args[0].equals("--data")) {
                file = Path.of(args[1]);
            } else if (args.length != 0) {
                System.out.println("Usage: SelfCheck [--interactive | --data file]");
                System.exit(2);
            }
            System.out.println("PR7 / variant 3");
            runUnitChecks();
            System.out.println("UNIT: " + passed + " PASS, " + failed + " FAIL");
            int unitPassed = passed;
            int unitFailed = failed;
            runDataChecks(file);
            System.out.println("DATA: " + (passed - unitPassed) + " PASS, "
                    + (failed - unitFailed) + " FAIL");
        } catch (Exception e) {
            check("runtime " + e.getMessage(), false);
        }
        System.out.println("TOTAL: " + passed + " PASS, " + failed + " FAIL");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS " + name);
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 0.000001;
    }

    private static Order newOrder() {
        return new OrderService().createOrder(new Employee("E1", "Test Employee"));
    }

    private static Order orderWithTotal(double sum) {
        Order order = newOrder();
        order.addProduct(new Product("P1", "Test Product", sum), 1);
        return order;
    }

    private static void productError(String label, String id, String name,
                                     double price, String message) {
        try {
            new Product(id, name, price);
            check(label, false);
        } catch (IllegalArgumentException e) {
            check(label, e.getMessage().startsWith(message));
        }
    }

    private static void itemError(String label, Product product, int quantity) {
        try {
            new OrderItem(product, quantity);
            check(label, false);
        } catch (IllegalArgumentException e) {
            check(label, true);
        }
    }

    private static void increaseError(String label, int quantity, int delta) {
        OrderItem item = new OrderItem(new Product("P", "Mouse", 1000), quantity);
        try {
            item.increase(delta);
            check(label, false);
        } catch (IllegalArgumentException e) {
            check(label, item.getQuantity() == quantity);
        }
    }

    private static void confirmError(String label, Order order, String message) {
        OrderStatus before = order.getStatus();
        try {
            order.confirm();
            check(label, false);
        } catch (IllegalStateException e) {
            check(label, e.getMessage().equals(message) && order.getStatus() == before);
        }
    }

    private static void categoryCheck(String label, double sum, String expected) {
        Order order = sum == 0 ? newOrder() : orderWithTotal(sum);
        check(label, new OrderService().addTotalCategory(order).equals(expected));
    }

    private static void inputError(String label, String line) {
        Order order = newOrder();
        try {
            addLine(order, line);
            check(label, false);
        } catch (IllegalArgumentException e) {
            check(label, order.getItems().isEmpty());
        }
    }

    private static void runUnitChecks() {
        Product mouse = new Product("M", "Mouse", 1000);
        Product keyboard = new Product("K", "Keyboard", 2000);
        check("unit/01 Product valid", mouse.getId().equals("M")
                && mouse.getName().equals("Mouse") && near(mouse.getPrice(), 1000));
        productError("unit/02 id null", null, "Mouse", 1000, "id is null or blank");
        productError("unit/03 id empty", "", "Mouse", 1000, "id is null or blank");
        productError("unit/04 name null", "P", null, 1000, "name is null or blank");
        productError("unit/05 price zero", "P", "Mouse", 0, "price must be > 0");
        productError("unit/06 price negative", "P", "Mouse", -100, "price must be > 0");
        OrderItem item = new OrderItem(mouse, 2);
        check("unit/07 OrderItem valid", item.getProduct() == mouse && item.getQuantity() == 2);
        itemError("unit/08 quantity zero", mouse, 0);
        itemError("unit/09 quantity 101", mouse, 101);
        check("unit/10 quantity 1", new OrderItem(mouse, 1).getQuantity() == 1);
        check("unit/11 quantity 100", new OrderItem(mouse, 100).getQuantity() == 100);
        item.increase(3);
        check("unit/12 increase 2 plus 3", item.getQuantity() == 5);
        Order order = newOrder();
        order.addProduct(mouse, 2);
        order.addProduct(keyboard, 3);
        check("unit/13 two products", order.getItems().size() == 2);
        Order merged = newOrder();
        merged.addProduct(mouse, 2);
        merged.addProduct(mouse, 1);
        check("unit/14 merge same id", merged.getItems().size() == 1
                && merged.getItems().get(0).getQuantity() == 3);
        check("unit/15 total two positions", near(order.total(), 8000));
        order.confirm();
        check("unit/16 confirm nonempty", order.getStatus() == OrderStatus.CONFIRMED);
        confirmError("unit/17 confirm empty", newOrder(), "order is empty");
        try {
            order.addProduct(mouse, 1);
            check("unit/18 add after confirm", false);
        } catch (IllegalStateException e) {
            check("unit/18 add after confirm", e.getMessage().equals("order is confirmed")
                    && near(order.total(), 8000));
        }

        Order limit = orderWithTotal(500000);
        limit.confirm();
        check("unit/19 variant A 500000 allowed", limit.getStatus() == OrderStatus.CONFIRMED);
        Order tooLarge = orderWithTotal(500000.01);
        confirmError("unit/20 variant A 500000.01 rejected", tooLarge,
                "order total exceeds 500000.0");
        categoryCheck("unit/21 variant B 9999.99", 9999.99, "малая");
        categoryCheck("unit/22 variant B 10000", 10000, "средняя");

        categoryCheck("extra/01 category 100000", 100000, "средняя");
        categoryCheck("extra/02 category 100000.01", 100000.01, "крупная");
        categoryCheck("extra/03 category 500000", 500000, "крупная");
        categoryCheck("extra/04 category 500000.01", 500000.01, "крупнейшая");
        categoryCheck("extra/05 category empty", 0, "малая");
        productError("extra/06 id blank", "  ", "Mouse", 1000, "id is null or blank");
        productError("extra/07 name empty", "P", "", 1000, "name is null or blank");
        productError("extra/08 price NaN", "P", "Mouse", Double.NaN, "price must be > 0");
        productError("extra/09 price infinity", "P", "Mouse", Double.POSITIVE_INFINITY,
                "price must be > 0");
        itemError("extra/10 product null", null, 1);
        itemError("extra/11 quantity negative", mouse, -1);
        check("extra/12 line total", near(new OrderItem(mouse, 3).getLineTotal(), 3000));
        increaseError("extra/13 increase zero", 2, 0);
        increaseError("extra/14 increase negative", 2, -1);
        increaseError("extra/15 increase over limit", 100, 1);
        increaseError("extra/16 increase int overflow", 2, Integer.MAX_VALUE);
        try {
            new Order(null);
            check("extra/17 employee null", false);
        } catch (NullPointerException e) {
            check("extra/17 employee null", e.getMessage().equals("employee is null"));
        }
        Order editable = newOrder();
        editable.addProduct(mouse, 2);
        editable.addProduct(keyboard, 3);
        editable.removeProduct("M");
        check("extra/18 remove recalculates total", editable.getItems().size() == 1
                && near(editable.total(), 6000));
        editable.removeProduct("missing");
        check("extra/19 remove missing", editable.getItems().size() == 1);
        try {
            order.removeProduct("M");
            check("extra/20 remove after confirm", false);
        } catch (IllegalStateException e) {
            check("extra/20 remove after confirm", near(order.total(), 8000));
        }
        confirmError("extra/21 confirm twice", order, "order is confirmed");
        try {
            order.getItems().add(new OrderItem(mouse, 1));
            check("extra/22 immutable list", false);
        } catch (UnsupportedOperationException e) {
            check("extra/22 immutable list", order.getItems().size() == 2);
        }
        order.getItems().get(0).increase(1);
        check("extra/23 confirmed item isolated", near(order.total(), 8000));
        List<OrderItem> snapshot = merged.getItems();
        merged.addProduct(mouse, 1);
        check("extra/24 snapshot isolated", snapshot.get(0).getQuantity() == 3
                && merged.getItems().get(0).getQuantity() == 4);
        try {
            merged.addProduct(mouse, 97);
            check("extra/25 failed merge unchanged", false);
        } catch (IllegalArgumentException e) {
            check("extra/25 failed merge unchanged", merged.getItems().get(0).getQuantity() == 4);
        }
        tooLarge.removeProduct("P1");
        tooLarge.addProduct(mouse, 1);
        tooLarge.confirm();
        check("extra/26 repair rejected order", tooLarge.getStatus() == OrderStatus.CONFIRMED);
        inputError("input/01 price text", "P;Mouse;abc;1");
        inputError("input/02 quantity text", "P;Mouse;1000;abc");
        inputError("input/03 surrounding spaces", "P; Mouse;1000;1");
        inputError("input/04 trailing separator", "P;Mouse;1000;1;");
        inputError("input/05 empty id", ";Mouse;1000;1");
    }

    private static void addLine(Order order, String line) {
        String[] fields = line.split(";", -1);
        if (fields.length != 4) {
            throw new IllegalArgumentException("expected 4 fields: id;name;price;quantity");
        }
        for (String field : fields) {
            if (!field.equals(field.strip())) {
                throw new IllegalArgumentException("surrounding spaces are not allowed");
            }
        }
        double price;
        int quantity;
        try {
            price = Double.parseDouble(fields[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("price is not a number");
        }
        try {
            quantity = Integer.parseInt(fields[3]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("quantity is not an integer");
        }
        order.addProduct(new Product(fields[0], fields[1], price), quantity);
    }

    private static OrderItem findItem(Order order, String id) {
        for (OrderItem item : order.getItems()) {
            if (item.getProduct().getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    private static boolean matches(String actual, String expected) {
        if (expected.equals("merged")) {
            return actual.startsWith("OK:merged,");
        }
        if (expected.equals("SKIP")) {
            return actual.startsWith("SKIP:");
        }
        if (expected.equals("ERROR")) {
            return actual.startsWith("SKIP:") || actual.startsWith("ERROR:");
        }
        if (expected.startsWith("SKIP:") && expected.length() > 5) {
            return actual.startsWith(expected);
        }
        return actual.equals(expected);
    }

    private static void runDataChecks(Path file) throws IOException {
        Order order = newOrder();
        int number = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")
                        || line.equals("productId;name;price;quantity;expected")) {
                    continue;
                }
                number++;
                String label = "data/" + number;
                try {
                    if (line.startsWith("TOTAL;")) {
                        checkTotal(label, order, line);
                        continue;
                    }
                    String[] fields = line.split(";", -1);
                    if (fields.length != 5) {
                        check(label + " expected 5 CSV fields", false);
                        continue;
                    }
                    boolean merged = findItem(order, fields[0]) != null;
                    String actual;
                    try {
                        addLine(order, fields[0] + ";" + fields[1] + ";" + fields[2] + ";" + fields[3]);
                        int quantity = findItem(order, fields[0]).getQuantity();
                        actual = "OK:" + (merged ? "merged," : "") + "qty=" + quantity;
                    } catch (IllegalArgumentException e) {
                        actual = "SKIP:" + e.getMessage();
                    } catch (IllegalStateException e) {
                        actual = "ERROR:IllegalStateException";
                    }
                    check(label + " " + actual, matches(actual, fields[4]));
                } catch (IllegalArgumentException e) {
                    check(label + " invalid expectation: " + e.getMessage(), false);
                }
            }
        }
        if (number == 0) {
            check("data file has no test cases", false);
        }
        printOrder(order);
    }

    private static String probeConfirm(Order order) {
        // Проверяем confirm на копии, чтобы продолжить чтение того же заказа из CSV.
        Order copy = new OrderService().createOrder(order.getEmployee());
        for (OrderItem item : order.getItems()) {
            copy.addProduct(item.getProduct(), item.getQuantity());
        }
        try {
            copy.confirm();
            return "OK";
        } catch (IllegalStateException e) {
            return "ERROR:IllegalStateException";
        }
    }

    private static void checkTotal(String label, Order order, String line) {
        String[] fields = line.split(";", -1);
        if (fields.length < 3 || fields.length > 5
                || !fields[1].startsWith("positions=") || !fields[2].startsWith("sum=")) {
            throw new IllegalArgumentException("TOTAL;positions=N;sum=X[;category=X][;confirm=X]");
        }
        int positions = Integer.parseInt(fields[1].substring(10));
        double sum = Double.parseDouble(fields[2].substring(4));
        boolean ok = positions == order.getItems().size() && near(sum, order.total());
        String category = new OrderService().addTotalCategory(order);
        String confirm = probeConfirm(order);
        boolean categorySeen = false;
        boolean confirmSeen = false;
        for (int i = 3; i < fields.length; i++) {
            if (fields[i].startsWith("category=") && !categorySeen) {
                ok = ok && fields[i].substring(9).equals(category);
                categorySeen = true;
            } else if (fields[i].startsWith("confirm=") && !confirmSeen) {
                ok = ok && fields[i].substring(8).equals(confirm);
                confirmSeen = true;
            } else {
                throw new IllegalArgumentException("unknown or duplicate TOTAL field");
            }
        }
        check(label + " TOTAL positions=" + order.getItems().size() + " category="
                + category + " confirm=" + confirm, ok);
    }

    private static void printOrder(Order order) {
        System.out.printf(Locale.ROOT, "positions=%d total=%.2f status=%s category=%s%n",
                order.getItems().size(), order.total(), order.getStatus(),
                new OrderService().addTotalCategory(order));
    }

    private static Order processCommand(Order order, String line) {
        if (line.equals("new")) {
            return newOrder();
        }
        if (line.equals("confirm")) {
            order.confirm();
        } else if (line.equals("total") || line.equals("category") || line.equals("show")) {
            // Итог выводится после каждой успешной команды.
        } else if (line.startsWith("remove;")) {
            String[] fields = line.split(";", -1);
            if (fields.length != 2 || !fields[1].equals(fields[1].strip())) {
                throw new IllegalArgumentException("expected remove;id without surrounding spaces");
            }
            order.removeProduct(fields[1]);
        } else if (line.startsWith("add;")) {
            addLine(order, line.substring(4));
        } else if (line.split(";", -1).length == 4) {
            addLine(order, line);
        } else {
            throw new IllegalArgumentException("unknown command");
        }
        return order;
    }

    private static void runInteractive() throws IOException {
        System.out.println("add;id;name;price;quantity | remove;id | total | category | confirm | show | new");
        System.out.println("Также: id;name;price;quantity. Выход: exit или пустая строка.");
        Order order = newOrder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty() && !line.equals("exit")) {
                try {
                    order = processCommand(order, line);
                    printOrder(order);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                }
            }
        }
    }
}
