/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ImageOptimizerPackage;

import java.io.*;
import java.util.*;
 
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.*;

/**
 *
 * @author Saswata
 */
public class ImageOptimizerServlet extends HttpServlet {

    private boolean isMultipart;
    private String filePath,iconPath;
    private final int maxFileSize = 1024*1024*5; //5MB
    private final int maxMemSize = 1024*1024*20;
    private File file ;
    private int count=0;

    @Override
    public void init( ){
       // Get the file location where it would be stored.
       filePath = getServletContext().getInitParameter("file-upload");
       iconPath = getServletContext().getInitParameter("fs-icon");
    }
    @Override
    public void doPost(HttpServletRequest request, 
                HttpServletResponse response)
               throws ServletException, java.io.IOException
    {
        count++; System.out.println("hello "+count);
       DiskFileItemFactory factory = new DiskFileItemFactory();
       // maximum size that will be stored in memory
       factory.setSizeThreshold(maxMemSize);
       // Location to save data that is larger than maxMemSize.
       factory.setRepository(new File("c:\\temp"));

       // Create a new file upload handler
       ServletFileUpload upload = new ServletFileUpload(factory);
       // maximum file size to be uploaded.
       upload.setSizeMax( maxFileSize );

       try{ 
            // Parse the request to get file items.
            List fileItems = upload.parseRequest(request);

            // Process the uploaded file items
            Iterator i = fileItems.iterator();
            
            FileItem fi = (FileItem)i.next();
            String fileName = fi.getName();
            String fullFilePath;
            // Write the file
            if(fileName.lastIndexOf("\\") >= 0)
            {
                fileName= fileName.substring(fileName.lastIndexOf("\\"));
                fullFilePath=filePath + fileName;
                file = new File( fullFilePath);
            }
            else
            {
                fileName= fileName.substring(fileName.lastIndexOf("\\")+1);
                fullFilePath=filePath + fileName;
                file = new File( fullFilePath);
            }
            fi.write( file );

            File watermarkImageFile = new File(iconPath);

            for(int c=0;c<=1;c++)
            {
                float newWidth,newHeight,per;
                if(c==0){newWidth=1400;newHeight=800;per=1f;}
                else{newWidth=400;newHeight=240;per=0.5f;}
                BufferedImage originalImage = ImageIO.read(file);
                int type = originalImage.getType() == 0? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
                BufferedImage resizeImageJpg = resizeImage(originalImage,type,newWidth,newHeight);
                String fileWithoutExt=fileName.substring(0, fileName.lastIndexOf(".")-1);
                String ext=fileName.substring(fileName.lastIndexOf("."));
                String newFile=filePath+fileWithoutExt+"_"+(int)newWidth+"x"+(int)newHeight+ext;
                ImageIO.write(resizeImageJpg, "jpg", new File(newFile));

                //Compression
                CompressImage(resizeImageJpg,newFile);

                //Watermark
                addImageWatermark(watermarkImageFile, new File(newFile), new File(newFile), per);
            }
            
            //response.sendRedirect("/index2.html?count="+count);
    }
    catch(Exception ex)
    {
        System.out.println(ex);
    }
    }
    @Override
    public void doGet(HttpServletRequest request, 
                        HttpServletResponse response)
         throws ServletException, java.io.IOException {

         throw new ServletException("GET method used with " +
                 getClass( ).getName( )+": POST method required.");
    }
    
    
    
    //Compress
    private void CompressImage(BufferedImage resizeImageJpg,String newFile) throws Exception
    {
        ImageWriter writer = (ImageWriter) ImageIO.getImageWritersBySuffix("jpg").next();
        writer.setOutput(ImageIO.createImageOutputStream(new File(newFile)));
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.5f);
        writer.write(null, new IIOImage(resizeImageJpg, null, null),param);
        writer.dispose();
    }
   
   
    //Resize
    private BufferedImage resizeImage(BufferedImage originalImage, int type, float newWidth, float newHeight)
    {
         BufferedImage resizedImage = new BufferedImage(Math.round(newWidth), Math.round(newHeight), type);
         Graphics2D g = resizedImage.createGraphics();
         
         float width=originalImage.getWidth(),height=originalImage.getHeight();
         int h=Math.round((newHeight/newWidth)*width);
         int t=Math.round((height-h)/2);
         
         g.drawImage(originalImage, 0, 0, Math.round(newWidth), Math.round(newHeight), 0, t, Math.round(width), Math.round(height-t), null);
         g.dispose();
         return resizedImage;
    }
    
    
    //Watermark
    private void addImageWatermark(File watermarkImageFile, File sourceImageFile, File destImageFile, float per)
    {
        try
        {
            BufferedImage sourceImage = ImageIO.read(sourceImageFile);
            BufferedImage watermarkImage = ImageIO.read(watermarkImageFile);

            // initializes necessary graphic properties
            Graphics2D g = (Graphics2D) sourceImage.getGraphics();
            AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f);
            g.setComposite(alphaChannel);

            // calculates the coordinate where the image is painted
            int width=(int)(watermarkImage.getWidth()*per);
            int height=(int)(watermarkImage.getHeight()*per);
            int topLeftX = (int)((sourceImage.getWidth() - width) / 2);
            int topLeftY = (int)((sourceImage.getHeight() - height) / 2);

            // paints the image watermark
            g.drawImage(watermarkImage, topLeftX, topLeftY, width, height, null);
            
            ImageIO.write(sourceImage, "jpg", destImageFile);
            g.dispose();
        }
        catch (IOException ex)
        {
            System.out.println(ex);
        }
    }
}
