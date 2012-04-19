package com.qwertovsky.mailer;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML.Attribute;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;

/**
 * Search IMG tag and return list of SRC attributes
 * @author Qwertovsky
 *
 */
public class ImageCallback extends ParserCallback
{
	private List<String> paths = new ArrayList<String>();
	
	@Override
	public void handleSimpleTag(Tag t, MutableAttributeSet a, int pos)
    {
		if(t == Tag.IMG)
		{
			String path = (String) a.getAttribute(Attribute.SRC);
			if(path.startsWith("cid:"))
				return;
			paths.add(path);
		}
    }
	//--------------------------------------------
	public List<String> getPathList()
	{
		return paths;
	}
}
