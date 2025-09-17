package io.github.studiousxiaoyu.chat;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONUtil;
import io.github.studiousxiaoyu.chat.domain.ActorFilms;
import io.github.studiousxiaoyu.chat.domain.StreamResponse;
import io.github.studiousxiaoyu.chat.function.DateTimeTools;
import io.github.studiousxiaoyu.hunyuan.audio.HunYuanAudioTextToVoiceModel;
import io.github.studiousxiaoyu.hunyuan.audio.HunYuanAudioTranscriptionModel;
import io.github.studiousxiaoyu.hunyuan.chat.HunYuanChatOptions;
import io.github.studiousxiaoyu.hunyuan.chat.message.HunYuanAssistantMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Slf4j
@RestController
public class ChatClientExample {

    private final ChatClient chatClient;

    private final HunYuanAudioTranscriptionModel audioTranscriptionModel;

    private final HunYuanAudioTextToVoiceModel textToVoiceModel;


    public ChatClientExample(ChatModel chatModel, HunYuanAudioTranscriptionModel audioTranscriptionModel, HunYuanAudioTextToVoiceModel textToVoiceModel) {
        this.chatClient = ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor()).build();
        this.audioTranscriptionModel = audioTranscriptionModel;
        this.textToVoiceModel = textToVoiceModel;
    }
    /**
     * 当前用户输入后，返回文本类型的回答
     * @return
     */
    @PostMapping("/chat")
    public String chat(@RequestParam("userInput")  String userInput) {
        String content = this.chatClient.prompt()
                .user(userInput)
                .call()
                .content();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 当前用户输入后，返回文本类型的回答，流式回答
     * @return
     */
    @GetMapping("/chat-stream")
    public Flux<ServerSentEvent<String>> chatStream(@RequestParam("userInput") String userInput) {
        return chatClient.prompt()
                .user(userInput)
                .stream()
                .content() // 获取原始Flux<String>
                .map(content -> ServerSentEvent.<String>builder() // 封装为SSE事件
                        .data(content)
                        .build());
    }

    @PostMapping("/chatWithPic")
    public String chatWithPic(@RequestParam("userInput")  String userInput) {
        var imageData = new ClassPathResource("/img.png");
        var userMessage = UserMessage.builder()
                .text(userInput)
                .media(List.of(new Media(MimeTypeUtils.IMAGE_PNG, imageData)))
                .build();
        var hunyuanChatOptions = HunYuanChatOptions.builder().model("hunyuan-turbos-vision").build();
        String content = this.chatClient.prompt(new Prompt(userMessage, hunyuanChatOptions))
                .call()
                .content();
        log.info("content: {}", content);
        return content;
    }

    //https://cloudcache.tencent-cloud.com/qcloud/ui/portal-set/build/About/images/bg-product-series_87d.png
    @PostMapping("/chatWithPicUrl")
    public String chatWithPicUrl(@RequestParam("url")  String url,@RequestParam("userInput")  String userInput) throws MalformedURLException {
        var imageData = new UrlResource(url);
        var userMessage = UserMessage.builder()
                .text(userInput)
                .media(List.of(Media.builder()
                        .mimeType(MimeTypeUtils.IMAGE_PNG)
                        .data(url)
                        .build()
                ))
                .build();
        var hunyuanChatOptions = HunYuanChatOptions.builder().model("hunyuan-t1-vision").build();
        String content = this.chatClient.prompt(new Prompt(userMessage, hunyuanChatOptions))
                .call()
                .content();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 当前用户输入后，返回一个实体类型的回答，
     * @return ActorFilms
     */
    @GetMapping("/ai-Entity")
    public ActorFilms aiEntity() {
        ActorFilms actorFilms = chatClient.prompt()
                .user("Generate the filmography for a random actor.")
                .call()
                .entity(ActorFilms.class);
        return actorFilms;
    }

    /**
     *当前用户输入后，返回列表实体类型的回答，ParameterizedTypeReference是一个泛型，用于指定返回的类型。
     * @return List<ActorFilms>
     */
    @GetMapping("/ai-EntityList")
    List<ActorFilms> generationByEntityList() {
        List<ActorFilms> actorFilms = chatClient.prompt()
                .user("Generate the filmography of 5 movies for Tom Hanks and Bill Murray.")
                .call()
                .entity(new ParameterizedTypeReference<List<ActorFilms>>() {
                });
        return actorFilms;
    }

    @PostMapping("/ai-function")
    String functionGenerationByText(@RequestParam("userInput")  String userInput) {
        HunYuanChatOptions options = new HunYuanChatOptions();
        options.setModel("hunyuan-functioncall");
        String content = this.chatClient
                .prompt()
                .options(options)
                .user(userInput)
                .tools(new DateTimeTools())
                .call()
                .content();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 思考链
     * @return
     */
    @PostMapping("/chat-think")
    public String think(@RequestParam("userInput")  String userInput) {
        HunYuanChatOptions options = new HunYuanChatOptions();
        options.setModel("hunyuan-a13b");
        options.setEnableThinking(true);
        ChatResponse chatResponse = this.chatClient.prompt()
                .user(userInput)
                .options(options)
                .call().chatResponse();
        HunYuanAssistantMessage output = (HunYuanAssistantMessage) chatResponse.getResult().getOutput();
        String think = output.getReasoningContent();
        String text = output.getText();
        log.info("think: {}", think);
        log.info("text: {}", text);
        return "think:" + think + "\n\ntext:" + text;
    }

    @PostMapping("/stream-think")
    public Flux<ServerSentEvent<String>> streamThink (@RequestParam("userInput") String userInput){
        HunYuanChatOptions options = new HunYuanChatOptions();
        options.setModel("hunyuan-a13b");
        options.setEnableThinking(true);
        Flux<ServerSentEvent<String>> chatResponse = this.chatClient.prompt()
                .user(userInput)
                .options(options)
                .stream()
                .chatResponse()
                .map(content -> (HunYuanAssistantMessage) content.getResult().getOutput())
                .map(content -> {
                    String think = content.getReasoningContent();
                    String text = content.getText();

                    StreamResponse streamResponse;
                    if (think != null && !think.isEmpty()) {
                        streamResponse = new StreamResponse("thinking", think);
                    } else {
                        streamResponse = new StreamResponse("answer", text);
                    }
                    return ServerSentEvent.<String>builder()
                            .data(JSONUtil.toJsonStr(streamResponse))
                            .build();
                });
        return chatResponse;
    }

    //https://output.lemonfox.ai/wikipedia_ai.mp3
    @PostMapping("/audio2textByUrl")
    public String audio2textByUrl(@RequestParam("url")  String url) throws MalformedURLException {
        Resource resource = new UrlResource(url);
        String call = audioTranscriptionModel.call(resource);
        log.info("text: {}", call);
        return call;
    }

    @PostMapping("/audio2textByPath")
    public String audio2textByPath(){
        Resource resource = new ClassPathResource("/speech/speech1.mp3");
        String call = audioTranscriptionModel.call(resource);
        log.info("text: {}", call);
        return call;
    }

    @PostMapping("/text2audio")
    public byte[] text2audio(@RequestParam("userInput")  String userInput) throws MalformedURLException {
        byte[] call = textToVoiceModel.call(userInput);
        FileUtil.writeBytes(call, "D:/output.mp3");
        return call;
    }
}
