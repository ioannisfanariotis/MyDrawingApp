package com.example.mydrawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import java.util.*

class DrawingView(context: Context, attrs: AttributeSet): View(context, attrs) {

    private var drawPath: CustomPath? = null
    private var canvasBitmap: Bitmap? = null
    private var drawPaint: Paint? = null
    private var canvasPaint: Paint? = null
    private var brushSize: Float = 0.toFloat()
    private var color = Color.BLACK
    private var canvas: Canvas? = null
    private val paths = ArrayList<CustomPath>()
    private val undoPaths = ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }

    private fun setUpDrawing(){
        drawPaint = Paint()
        drawPaint!!.color = color
        drawPaint!!.style = Paint.Style.STROKE
        drawPaint!!.strokeJoin = Paint.Join.ROUND
        drawPaint!!.strokeCap = Paint.Cap.ROUND
        drawPath = CustomPath(color, brushSize)
        canvasPaint = Paint(Paint.DITHER_FLAG)

    }

    fun onClickUndo(){
        if(paths.size > 0){
            undoPaths.add(paths.removeAt(paths.size - 1))
            invalidate()
        }
    }

    fun onClickRedo(){
        if(undoPaths.size > 0){
            paths.add(undoPaths.removeAt(undoPaths.size - 1))
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(canvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas){
        super.onDraw(canvas)
        canvas.drawBitmap(canvasBitmap!!, 0f, 0f, canvasPaint)

        for(i in paths){
            drawPaint!!.strokeWidth = i.thick
            drawPaint!!.color = i.color
            canvas.drawPath(i, drawPaint!!)
        }

        if(!drawPath!!.isEmpty){
            drawPaint!!.strokeWidth = drawPath!!.thick
            drawPaint!!.color = drawPath!!.color
            canvas.drawPath(drawPath!!, drawPaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                drawPath?.color = color
                drawPath?.thick = brushSize

                drawPath!!.reset()
                drawPath!!.moveTo(touchX!!, touchY!!)
            }
            MotionEvent.ACTION_MOVE -> {
                drawPath!!.lineTo(touchX!!, touchY!!)
            }
            MotionEvent.ACTION_UP -> {
                paths.add(drawPath!!)
                drawPath = CustomPath(color, brushSize)
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun setBrushSize(newSize: Float){
        brushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, resources.displayMetrics)
        drawPaint!!.strokeWidth = brushSize
    }

    fun setColor(newColor: String){
        color=Color.parseColor(newColor)
        drawPaint!!.color = color
    }

    internal inner class CustomPath(var color: Int, var thick: Float):Path(){}
}