package com.example.aiplayground;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
public class DemoController {

    private static final String INSTRUCTIONS = """
            GOLD membership requires verified total spending >= 10000.
            Never invent spending values or member records.
            If a getMember tool is available, call it to obtain member data before answering.
            If the tool is unavailable and no member data is given, say you cannot verify eligibility.
            If the tool says the member was not found, say so.
            Explain which facts support your answer.
            """;

    private final ChatClient chatClient;
    private final MemberTools memberTools;

    public DemoController(ChatClient.Builder builder, MemberTools memberTools) {
        // No default tools: the plain LLM endpoint remains isolated.
        this.chatClient = builder.build();
        this.memberTools = memberTools;
    }

    @GetMapping("/llm")
    public String llmOnly(@RequestParam(defaultValue = "M100") String memberId) {
        return chatClient.prompt()
                .system(INSTRUCTIONS)
                .user("Is member " + memberId + " eligible for GOLD membership?")
                .call()
                .content();
    }

    @GetMapping("/agent")
    public String withTool(@RequestParam(defaultValue = "M100") String memberId) {
        return chatClient.prompt()
                .system(INSTRUCTIONS)
                .user("Is member " + memberId + " eligible for GOLD membership?")
                .tools(memberTools)
                .call()
                .content();
    }
}
