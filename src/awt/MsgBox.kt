package awt


import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JLabel

/** A convenient message box class to pop up here and there.
 *
 * @author zyklon
 */
class MsgBox(frame: Frame?, title: String?, msg: String?, okcan: Boolean) : Dialog(frame, title, true), ActionListener {
    private var ok: Button? = null
    private var can: Button? = null
    private var isOk = false

    /*  
	 * * @param frame parent frame
	 * 
	 * @param msg message to be displayed
	 * 
	 * @param okcan true : ok cancel buttons, false : ok button only
	 */
    fun isOk(): Boolean {
        return isOk
    }

    init {
        layout = BorderLayout()
        add("Center", JLabel(msg))
        addOKCancelPanel(okcan)
        createFrame()
        pack()
        isVisible = true
        can!!.requestFocus()
    }

    constructor(frame: Frame?, msg: String?) : this(frame, "Message", msg, false) {}

    private fun addOKCancelPanel(okcan: Boolean) {
        val p = Panel()
        p.layout = FlowLayout()
        createOKButton(p)
        if (okcan == true) createCancelButton(p)
        add("South", p)
    }

    private fun createOKButton(p: Panel) {
        p.add(Button("OK").also { ok = it })
        ok!!.addActionListener(this)
    }

    private fun createCancelButton(p: Panel) {
        p.add(Button("Cancel").also { can = it })
        can!!.addActionListener(this)
    }

    private fun createFrame() {
        val d = toolkit.screenSize
        setLocation(d.width / 3, d.height / 3)
    }

    override fun actionPerformed(ae: ActionEvent) {
        if (ae.source === ok) {
            isOk = true
            isVisible = false
        } else if (ae.source === can) {
            isVisible = false
        }
    } /*
	 * public static void main(String args[]) { //Frame f = new Frame();
	 * //f.setSize(200,200); //f.setVisible(true); MsgBox message = new MsgBox
	 * (null , "Hey you user, are you sure ?", true); if (message.isOk)
	 * System.out.println("Ok pressed"); if (!message.isOk)
	 * System.out.println("Cancel pressed"); message.dispose(); }
	 */

    companion object {
        /**
         *
         */
        private const val serialVersionUID = -872019680203708495L
    }
}