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
import java.net.URL;
import javax.imageio.*;


/*import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;*/

/**
 *
 * @author Saswata
 */
public class demoServlet extends HttpServlet
{
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        File file ;
        int maxFileSize = 5000 * 1024;
        int maxMemSize = 5000 * 1024;
        String filePath = "c:\\hi\\";
        
        // Verify the content type
        String contentType = request.getContentType();
        if ((contentType.indexOf("multipart/form-data") >= 0)) {

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
              
              BufferedImage watermarkImage = ImageIO.read(new URL("https://www.findspace.in/images/fS.png"));
              
              while ( i.hasNext () ) 
              {
                 FileItem fi = (FileItem)i.next();
                 if ( !fi.isFormField () )	
                 {
                    // Get the uploaded file parameters
                    String fieldName = fi.getFieldName();
                    String fileName = fi.getName();
                    boolean isInMemory = fi.isInMemory();
                    long sizeInBytes = fi.getSize();
                    
                    // Write the file
                    if( fileName.lastIndexOf("\\") >= 0 ){
                    file = new File( filePath + 
                    fileName.substring( fileName.lastIndexOf("\\"))) ;
                    }else{
                    file = new File( filePath + 
                    fileName.substring(fileName.lastIndexOf("\\")+1)) ;
                    }
                    
                    
                    for(int c=0;c<=1;c++)
                    {
                        float newWidth,newHeight,per;
                        if(c==0)
                            {newWidth=1400;newHeight=800;per=1f;}
                        else
                            {newWidth=400;newHeight=240;per=0.5f;}
                        BufferedImage originalImage = ImageIO.read(fi.getInputStream());
                        int type = originalImage.getType() == 0? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
                        BufferedImage resizeImageJpg = resizeImage(originalImage,type,newWidth,newHeight);
                        String fileWithoutExt=fileName.substring(0, fileName.lastIndexOf(".")-1);
                        String ext=fileName.substring(fileName.lastIndexOf("."));
                        String newFile=filePath+fileWithoutExt+"_"+(int)newWidth+"x"+(int)newHeight+ext;

                        //Compression
                        BufferedImage compressedImage=CompressImage(resizeImageJpg,newFile);

                        //Watermark
                        addImageWatermark(watermarkImage, compressedImage, new File(newFile), per);
                    }
                 }
                 else
                 {
                     System.out.println(fi.getFieldName()+": "+fi.getString());
                 }
              }
           }catch(Exception ex) {
              System.out.println(ex);
           }
        }
    }
    
    
    //Compress
    private BufferedImage CompressImage(BufferedImage resizeImageJpg,String newFile) throws Exception
    {
        ImageWriter writer = (ImageWriter) ImageIO.getImageWritersBySuffix("jpg").next();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(baos));
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.5f);
        writer.write(null, new IIOImage(resizeImageJpg, null, null),param);
        writer.dispose();
        baos.flush();
        byte[] imgByte=baos.toByteArray();
        baos.close();
        InputStream in = new ByteArrayInputStream(imgByte);
	return (BufferedImage)ImageIO.read(in);
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
    private void addImageWatermark(BufferedImage watermarkImage, BufferedImage compressedImage, File destImageFile, float per)
    {
        try
        {
            BufferedImage sourceImage = compressedImage;

            // initializes necessary graphic properties
            Graphics2D g = (Graphics2D) sourceImage.getGraphics();
            AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f);
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
