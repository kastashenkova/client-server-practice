package org.example.practice2.processor;

import org.example.practice1.Message;
import org.example.practice2.SharedQueue;
import org.example.practice2.warehouse.CommandResult;
import org.example.practice2.warehouse.WarehouseCommand;
import org.example.practice2.warehouse.WarehouseService;
import org.example.practice4.Filter;
import org.example.practice4.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class Processor implements Runnable {
    private final SharedQueue<Message> inputQueue;
    private final SharedQueue<Message> outputQueue;
    private final WarehouseService warehouseService;
    private volatile boolean active = true;

    public Processor(SharedQueue<Message> inputQueue,
                     SharedQueue<Message> outputQueue,
                     WarehouseService warehouseService) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.warehouseService = warehouseService;
    }

    public void process(Message message) {
        WarehouseCommand command = WarehouseCommand.parse(
                message.commandId(), message.messageString());
        CommandResult result = execute(command);

        String responseText = result.success()
                ? "OK: " + result.message()
                : "ERROR: " + result.message();

        Message response = new Message(
                message.uniqueIdentifier(),
                message.messageNumber(),
                message.commandId(),
                message.userId(),
                responseText
        );

        try {
            outputQueue.produce(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private CommandResult execute(WarehouseCommand command) {
        try {
            return switch (command.type()) {
                case GET_PRODUCT_QUANTITY -> {
                    String product = command.params()[0];
                    int quantity = warehouseService.getStock(product);
                    yield CommandResult.success(product + " quantity = " + quantity);
                }
                case ADD_PRODUCTS -> {
                    String product = command.params()[0];
                    int quantity = Integer.parseInt(command.params()[1]);
                    int total = warehouseService.addProducts(product, quantity);
                    yield CommandResult.success("Added " + quantity + " of " + product + ", total = " + total);
                }
                case DEDUCT_PRODUCTS -> {
                    String product = command.params()[0];
                    int quantity = Integer.parseInt(command.params()[1]);
                    int remainder = warehouseService.deductProducts(product, quantity);
                    yield CommandResult.success("Deducted " + quantity + " of " + product + ", remainder = " + remainder);
                }
                case ADD_GROUP -> {
                    warehouseService.addGroup(command.params()[0]);
                    yield CommandResult.success("Group created: " + command.params()[0]);
                }
                case ADD_PRODUCT_NAME_TO_GROUP -> {
                    warehouseService.addProductToGroup(command.params()[0], command.params()[1]);
                    yield CommandResult.success("Added " + command.params()[1] + " to group " + command.params()[0]);
                }
                case SET_PRODUCT_PRICE -> {
                    double price = Double.parseDouble(command.params()[1]);
                    warehouseService.setPrice(command.params()[0], price);
                    yield CommandResult.success("New " + command.params()[0] + " price = " + price);
                }
                case SEARCH_PRODUCTS -> {
                    Filter filter = new Filter();
                    for (String param : command.params()) {
                        String[] parts = param.split(":");
                        if (parts.length == 2) {
                            switch (parts[0]) {
                                case "name" -> filter.name = parts[1];
                                case "minPrice" -> filter.minPrice = new BigDecimal(parts[1]);
                                case "maxPrice" -> filter.maxPrice = new BigDecimal(parts[1]);
                                case "minQuantity" -> filter.minQuantity = Integer.parseInt(parts[1]);
                                case "maxQuantity" -> filter.maxQuantity = Integer.parseInt(parts[1]);
                                case "limit" -> filter.limit = Integer.parseInt(parts[1]);
                            }
                        }
                    }

                    List<Product> products = warehouseService.searchProducts(filter);
                    String resultList = products.stream()
                            .map(p -> p.getName() + "(ID:" + p.getId() + ")")
                            .collect(Collectors.joining(", "));

                    yield CommandResult.success("Found products: " + (resultList.isEmpty() ? "none" : resultList));
                }

                case DELETE_PRODUCT -> {
                    int id = Integer.parseInt(command.params()[0]);
                    boolean deleted = warehouseService.deleteProduct(id);

                    if (deleted) {
                        yield CommandResult.success("Deleted product with ID: " + id);
                    } else {
                        yield CommandResult.error("Product not found with ID: " + id);
                    }
                }
            };
        } catch (Exception e) {
            String errorDetails = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return CommandResult.error("Command failed: " + command.type() + " | Error: " + errorDetails);
        }
    }

    @Override
    public void run() {
        while (active && !Thread.currentThread().isInterrupted()) {
            try {
                process(inputQueue.consume());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void stop() {
        active = false;
    }
}
