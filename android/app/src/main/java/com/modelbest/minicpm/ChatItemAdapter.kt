package com.modelbest.minicpm

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class ChatItemAdapter(context: Context?) : BaseAdapter() {
    private var inflater: LayoutInflater? = null
    init {
        inflater = LayoutInflater.from(context)
    }
    override fun getCount(): Int {
        //return mApi.chatItems.size();
        return Utils.appViewModel?.chatState?.messages?.size!!
    }

    override fun getItem(i: Int): Any? {
        //return mApi.chatItems.get(i);
        return Utils.appViewModel?.chatState?.messages?.get(i)
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(position: Int, view: View?, viewGroup: ViewGroup?): View? {
        var contentView = view
        val holder: ViewHolder?
        val item:MessageData? = getItemAtPos(position)
        //Log.e("ITEM   ", item?.role.toString() + " : " + item?.text)
        if (contentView == null) {
            holder = ViewHolder()
            if (item?.role == MessageRole.Bot) {
                contentView = inflater!!.inflate(R.layout.chatview_bot, null)
                holder.textView = contentView.findViewById(R.id.text_bot_in)
                holder.textView?.text = item.text.trim()
            } else if(item?.role == MessageRole.User){
                if(item.text != "" && item.image_path == ""){
                    contentView = inflater!!.inflate(R.layout.chatview_user, null)
                    holder.textView = contentView.findViewById(R.id.text_user_in)
                    holder.textView?.text = item.text.trim()
                }else if (item.text == "" && item.image_path != ""){
                    contentView = inflater!!.inflate(R.layout.chatview_img, null)
                    holder.imgSrc = contentView.findViewById(R.id.img_src)
                    holder.imgSrc!!.setImageBitmap(BitmapFactory.decodeFile(item.image_path))
                    holder.imgSrc!!.setOnClickListener {
                        Utils.showImg(contentView.context, item.image_path)
                    }
                }
            }
            contentView!!.tag = holder
        } else {
            holder = contentView.tag as ViewHolder
        }
        return contentView
    }

    private fun getItemAtPos(position: Int): MessageData? {
        return Utils.appViewModel?.chatState?.messages?.get(position)
    }
    class ViewHolder {
        var textView: TextView? = null
        var imgSrc:ImageView? = null
    }
}