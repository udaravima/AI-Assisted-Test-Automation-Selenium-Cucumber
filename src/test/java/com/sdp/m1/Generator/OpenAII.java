package com.sdp.m1.Generator;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatModel;

public class OpenAII {
    public static void main(String[] args) {
        // Export your OpenAI API key as an environment variable named OPENAI_API_KEY
        OpenAIClient client = OpenAIOkHttpClient.fromEnv();
        ChatModel model = ChatModel.GPT_4O_MINI;
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessage("Say this is a test")
                .model(model)
                .build();
        client.chat().completions().create(params);
    }
}
