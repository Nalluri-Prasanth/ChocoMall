package com.chocomall.core.order;

import com.chocomall.common.exception.Asserts;
import com.chocomall.model.entity.OrderItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Translates order lines into base units per product. A normal line consumes
 * {@code quantity x unitsPerSku} units of its product; a build-a-box line consumes the pieces of the
 * products chosen for each box.
 */
@Component
@RequiredArgsConstructor
public class OrderItemUnits {

    private final ObjectMapper mapper;

    public Map<Long, Integer> unitsByProduct(List<OrderItem> items) {
        Map<Long, Integer> units = new LinkedHashMap<>();
        for (OrderItem item : items) {
            if (item.getBoxSelection() != null && !item.getBoxSelection().isBlank()) {
                for (BoxLine line : parse(item.getBoxSelection())) {
                    units.merge(line.productId(), line.quantity() * item.getProductQuantity(), Integer::sum);
                }
            } else {
                units.merge(item.getProductId(), item.getProductQuantity() * item.getUnitsPerSku(), Integer::sum);
            }
        }
        return units;
    }

    public List<BoxLine> parse(String json) {
        try {
            return mapper.readValue(json, new TypeReference<List<BoxLine>>() { });
        } catch (JsonProcessingException e) {
            throw Asserts.notFound("Readable box selection");
        }
    }

    public String write(List<BoxLine> lines) {
        try {
            return mapper.writeValueAsString(lines);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
