package com.test.myspring

import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Controller
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {
    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic", "/game_1")
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/gs-guide-websocket")
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(object : ChannelInterceptor {
            override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
                val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
                if (accessor?.user == null) {
                    throw UsernameNotFoundException("Authentication failed")
                }
                return message
            }
        })
    }
}

data class HelloMessage(val name: String)
data class Greeting(val content: String)
data class AnswerMessage(
    val userId: String,
    val gameId: String,
    val questionId: String,
    val answer: String,
    val isCorrect: Boolean = false,
)

@Controller
class SocketController {
    private final val questions: List<Question> = OBJECT_MAPPER.readValue(
        javaClass.classLoader.getResourceAsStream("question.json"),
        TypeFactory.defaultInstance().constructCollectionType(List::class.java, Question::class.java),
    )

    @Autowired
    private lateinit var messagingTemplate: SimpMessagingTemplate
    private var currentQuestion: Question? = null

    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    fun greeting(message: HelloMessage): Greeting {
        return Greeting("Hello, " + message.name)
    }

    @MessageMapping("/game_1/start")
    fun startGame1() {
        currentQuestion = questions.random()
        messagingTemplate.convertAndSend("/game_1/show_question", currentQuestion!!)
    }

    @MessageMapping("/game_1/answer")
    fun answerGame1(message: AnswerMessage) {
        if (message.questionId != currentQuestion?.id) {
            return
        }
        val isCorrect = questions.firstOrNull { it.id == message.questionId }?.correct == message.answer
        messagingTemplate.convertAndSend("/game_1/answer", message.copy(isCorrect = isCorrect))
//        Thread.sleep(1000)
        startGame1()
    }
}

val OBJECT_MAPPER = jacksonObjectMapper()

fun parseQuestion() {
    val data = "1. Câu hỏi: Thành phố nào là thủ đô của Pháp?\n" +
        "   A. Berlin\n" +
        "   B. Washington D.C.\n" +
        "   C. Paris (Đúng)\n" +
        "   D. London\n"
    val questions = ArrayList<Question>()
    data.split(Regex("\\n.*Câu hỏi: ")).forEachIndexed { index, text ->
        val array = text.split("\n")
        try {
            questions.add(
                Question(
                    id = (index + 1).toString(),
                    question = array[0],
                    a = array[1].replace(Regex(" .*[ABCD]. "), "").replace(" (Đúng)", ""),
                    b = array[2].replace(Regex(" .*[ABCD]. "), "").replace(" (Đúng)", ""),
                    c = array[3].replace(Regex(" .*[ABCD]. "), "").replace(" (Đúng)", ""),
                    d = array[4].replace(Regex(" .*[ABCD]. "), "").replace(" (Đúng)", ""),
                    correct = when {
                        array[1].contains("(Đúng)") -> "a"
                        array[2].contains("(Đúng)") -> "b"
                        array[3].contains("(Đúng)") -> "c"
                        array[4].contains("(Đúng)") -> "d"
                        else -> return@forEachIndexed
                    },
                )
            )
        } catch (e: Exception) {
            println("error at $text")
        }
    }
    println(OBJECT_MAPPER.writer().withDefaultPrettyPrinter().writeValueAsString(questions))
}

data class Question(
    val id: String,
    val question: String,
    val a: String,
    val b: String,
    val c: String,
    val d: String,
    val correct: String,
)