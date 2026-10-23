package com.example.aiplayground;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class MemberTools {

    private static final Logger log = LoggerFactory.getLogger(MemberTools.class);

    // Our pretend "business database". No persistence or network calls.
    private final Map<String, Integer> totalSpending = Map.of(
            "M100", 12_000,
            "M200", 3_000
    );

    @Tool(description = "Look up the verified total spending of a member by member ID. "
            + "Call this before deciding whether a specific member qualifies for GOLD membership.")
    public MemberData getMember(
            @ToolParam(description = "Member ID, such as M100") String memberId) {

        log.info("TOOL EXECUTED: getMember({})", memberId);
        Integer amount = totalSpending.get(memberId);
        return new MemberData(memberId, amount != null, amount);
    }

    public record MemberData(String memberId, boolean found, Integer totalSpent) {
    }
}
