package com.jason.mypersonalai.tools.impl

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.jason.mypersonalai.tools.RiskLevel
import com.jason.mypersonalai.tools.Tool
import com.jason.mypersonalai.tools.ToolError
import com.jason.mypersonalai.tools.ToolExecutionResult
import com.jason.mypersonalai.tools.ToolInput

/** Android-native navigation helpers. It never bypasses Android permission/security boundaries. */
class AndroidAssistantTool(private val context: Context) : Tool {
    override val id = "android_assistant"
    override val description = "Contacts, call history, files, media, Wi-Fi and Bluetooth navigation."
    override val riskLevel = RiskLevel.LOW

    override suspend fun execute(input: ToolInput): ToolExecutionResult {
        val text=input.raw.trim().lowercase()
        return when {
            text.contains("missed calls") || text.contains("recent calls") || text.contains("call history") || text.contains("call log") -> callLog(text)
            text.contains("who has this number") || text.contains("find a contact") || text == "contacts" -> contacts(text)
            text.contains("wifi") || text.contains("wi-fi") || text.contains("bluetooth") -> connectivity(text)
            text.contains("pictures") || text.contains("photos") || text.contains("music") || text.contains("songs") || text.contains("media") -> media(text)
            text.contains("files") || text.contains("documents") || text.contains("downloads") -> files()
            else -> ToolExecutionResult.Failure(ToolError("I don't have permission or a supported Android action for that request."))
        }
    }

    private fun launch(intent: Intent, success: String, failure: String): ToolExecutionResult = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        ToolExecutionResult.Success(success)
    } catch (t: Throwable) { ToolExecutionResult.Failure(ToolError(failure, t)) }

    private fun connectivity(text:String):ToolExecutionResult {
        val bluetooth=text.contains("bluetooth")
        if(bluetooth) {
            val action=when { text.contains("nearby")||text.contains("scan")||text.contains("discover") -> Settings.ACTION_BLUETOOTH_SETTINGS; else -> Settings.ACTION_BLUETOOTH_SETTINGS }
            return launch(Intent(action), "Opening Bluetooth. Would you like to scan, connect, or change its state?", "Android did not permit me to open Bluetooth settings.")
        }
        return launch(Intent(Settings.ACTION_WIFI_SETTINGS), "Opening Wi-Fi. Would you like to turn it on, turn it off, or check its status?", "Android did not permit me to open Wi-Fi settings.")
    }

    private fun files():ToolExecutionResult {
        val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="*/*"; addCategory(Intent.CATEGORY_OPENABLE) }
        return launch(intent, "I opened the Android file picker. Tell me what you want to access.", "Android did not permit me to open the document picker.")
    }

    private fun media(text:String):ToolExecutionResult {
        val type=when { text.contains("music")||text.contains("song") -> "audio/*"; text.contains("picture")||text.contains("photo") -> "image/*"; else -> "*/*" }
        return launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { this.type=type; addCategory(Intent.CATEGORY_OPENABLE); putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true) }, "I opened the media picker. Tell me what you want to access.", "Android did not permit me to open the media picker.")
    }

    private fun contacts(text:String):ToolExecutionResult {
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)
            return ToolExecutionResult.Failure(ToolError("Android has not permitted MyPersonalAI to read your contacts. Grant Contacts permission, then ask again."))
        val callRequest=text.startsWith("call ") || text.startsWith("phone ")
        val number=Regex("(?:\\+?\\d[\\d ()-]{7,})").find(text)?.value?.replace(Regex("[^0-9+]"),"")
        val cursor=context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER),null,null,null)
            ?: return ToolExecutionResult.Failure(ToolError("Android did not return your contacts."))
        cursor.use { c ->
            val matches=mutableListOf<Pair<String,String>>()
            while(c.moveToNext()) { val name=c.getString(0)?:"Unknown"; val phone=c.getString(1)?:""; if(number==null || normalized(phone)==normalized(number)) matches += name to phone }
            if(matches.isEmpty()) return ToolExecutionResult.Success(if(number!=null) "I didn't find a contact with that number." else "I couldn't find a matching contact.")
            val unique=matches.distinctBy{it.first+normalized(it.second)}.take(5)
            if (callRequest && unique.size == 1) {
                val uri=Uri.parse("tel:${unique[0].second}")
                return if(ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)==PackageManager.PERMISSION_GRANTED) launch(Intent(Intent.ACTION_CALL,uri), "Calling ${unique[0].first}.", "Android did not permit me to place the call.")
                else launch(Intent(Intent.ACTION_DIAL,uri), "Android has not permitted direct calling, so I opened the dialer for ${unique[0].first}.", "Android did not permit me to open the dialer.")
            }
            return ToolExecutionResult.Success(if(unique.size==1) "I found ${unique[0].first} at ${unique[0].second}." else "I found multiple matching contacts: ${unique.joinToString { it.first+" ("+it.second+")" }}. Which one do you mean?")
        }
    }

    private fun callLog(text:String):ToolExecutionResult {
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)!=PackageManager.PERMISSION_GRANTED)
            return ToolExecutionResult.Failure(ToolError("Android has not permitted MyPersonalAI to read your call history. Grant Call Log permission, then ask again."))
        val cursor=context.contentResolver.query(CallLog.Calls.CONTENT_URI,arrayOf(CallLog.Calls.NUMBER,CallLog.Calls.TYPE,CallLog.Calls.DATE),null,null,CallLog.Calls.DATE+" DESC LIMIT 10")
            ?: return ToolExecutionResult.Failure(ToolError("Android did not return your call history."))
        cursor.use { c ->
            var count=0; var missed=0
            while(c.moveToNext()) { count++; if(c.getInt(1)==CallLog.Calls.MISSED_TYPE) missed++ }
            return ToolExecutionResult.Success(if(text.contains("missed")) "You have $missed missed call(s) in the latest call records." else "I found $count recent call record(s).")
        }
    }
    private fun normalized(value:String):String { val digits=value.filter(Char::isDigit); return if(digits.startsWith("254")) "0"+digits.drop(3) else digits }
}
