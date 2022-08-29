package ch.lucaro.speakeasyexample

import ch.ddis.speakeasy.client.ChatApi
import ch.ddis.speakeasy.client.UserApi
import codeanticode.eliza.Eliza
import org.openapitools.client.infrastructure.ClientException
import org.openapitools.client.models.ChatRoomInfo
import org.openapitools.client.models.LoginRequest

object Main {

    val basePath = "https://speakeasy.ifi.uzh.ch"

    @JvmStatic
    fun main(args: Array<String>) {

        if (args.size < 2) {
            println("expected user and password as parameters")
            return
        }

        val user = args[0]
        val password = args[1]

        val userApi = UserApi(basePath)
        val chatApi = ChatApi(basePath)

        val sessionDetails = userApi.postApiLogin(
            LoginRequest(
                user, password
            )
        )

        println(sessionDetails)

        val knownRooms = mutableSetOf<String>()

        while (true) {
            val rooms = chatApi.getApiRooms(sessionDetails.sessionToken).rooms.filter { it.remainingTime > 0 }

            rooms.forEach { room ->
                if (room.uid !in knownRooms) {
                    println("new chat room: ${room}")

                    knownRooms.add(room.uid)

                    Thread {

                        val roomId = room.uid
                        var active = true
                        var lastMessageTime = 0L

                        val eliza = Eliza()

                        while (active) {

                            try{

                            val state = chatApi.getApiRoomWithRoomidWithSince(
                                roomId,
                                lastMessageTime + 1,
                                sessionDetails.sessionToken
                            )

                            val lastMessage =
                                state.messages.lastOrNull { it.session != sessionDetails.sessionId }

                                if (lastMessage != null) {
                                lastMessageTime = lastMessage.timeStamp
                                val reply = eliza.processInput(lastMessage.message)
                                println("received message in room ${room.uid}")
                                println("message: '${lastMessage.message}'")
                                println("replying with: '${reply}'")
                                println()
                                chatApi.postApiRoomWithRoomid(roomId, sessionDetails.sessionToken, reply)
                            }

                            active = state.info.remainingTime > 0

                            } catch (e : ClientException) {
                                System.err.println("something went wrong:")
                                e.printStackTrace()
                            }

                            Thread.sleep(1000)
                        }

                    }.start()

                }
            }
            Thread.sleep(1000)
        }
    }
}