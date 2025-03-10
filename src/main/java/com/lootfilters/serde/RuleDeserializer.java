package com.lootfilters.serde;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.lootfilters.rule.AndRule;
import com.lootfilters.rule.ItemIdRule;
import com.lootfilters.rule.ItemNameRule;
import com.lootfilters.rule.ItemQuantityRule;
import com.lootfilters.rule.ItemValueRule;
import com.lootfilters.rule.OrRule;
import com.lootfilters.rule.ItemOwnershipRule;
import com.lootfilters.rule.Rule;
import lombok.AllArgsConstructor;

import java.lang.reflect.Type;

@AllArgsConstructor
public class RuleDeserializer implements JsonDeserializer<Rule> {
    private final Gson gson;

    @Override
    public Rule deserialize(JsonElement elem, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        var object = elem.getAsJsonObject();
        var discriminator = object.get("discriminator").getAsString();
        switch (discriminator) {
            case "item_id":
                return gson.fromJson(object, ItemIdRule.class);
            case "item_ownership":
                return gson.fromJson(object, ItemOwnershipRule.class);
            case "item_name":
                return gson.fromJson(object, ItemNameRule.class);
            case "item_value":
                return gson.fromJson(object, ItemValueRule.class);
            case "item_quantity":
                return gson.fromJson(object, ItemQuantityRule.class);
            case "and":
                return deserializeInner(object, AndRule.class);
            case "or":
                return deserializeInner(object, OrRule.class);
            default:
                throw new JsonParseException("unknown rule type " + discriminator);
        }
    }

    private Rule deserializeInner(JsonElement elem, Type type) throws JsonParseException {
        return gson.newBuilder()
                .registerTypeAdapter(Rule.class, this)
                .create()
                .fromJson(elem, type);
    }
}
