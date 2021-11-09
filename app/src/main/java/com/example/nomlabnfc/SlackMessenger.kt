package com.example.nomlabnfc

import com.slack.api.Slack
import com.slack.api.methods.response.chat.ChatPostMessageResponse

class SlackMessenger {
    private val slack = Slack.getInstance()

    fun postMessage(token: String, channelName: String, message: String): ChatPostMessageResponse {
        return slack.methods(token).chatPostMessage { req ->
            req.channel(channelName).text(message)
        }
    }

}