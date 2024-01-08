package com.grocottlab.webcamj;

/**
 * WebcamJ plugin for ImageJ and Fiji.
 * Copyright (C) 2023 Timothy Grocott 
 *
 * More information at http://www.grocottlab.com/software
 *
 * This file is part of WebcamJ.
 * 
 * WebcamJ is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * WebcamJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import ij.gui.GenericDialog;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import static java.lang.Thread.sleep;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.IplImage;

public class Webcam_J implements PlugIn {
    
    static JFrame jframe;
    String jframe_title = "WebcamJ";
    Color button_color;
    // Live button
    JButton live_button;
    boolean live = false;
    // Snap button
    JButton snap_button;
    // Montage button
    JButton montage_button;
    boolean montaging = false;
    Image[][] image_montage;
    int montage_rows;
    int montage_cols;
    int current_row;
    int current_col;
    // Stack button
    JButton stack_button;
    boolean stacking = false;
    Image[] image_stack;
    int stack_size;
    int stack_counter;
    // Crosshairs checkbox
    JCheckBox crosshair_checkbox;
    boolean crosshair = false;
    JCheckBox grid_checkbox;
    boolean grid = false;
    int grid_divisions_x = 3;
    int grid_divisions_y = 3;
    JCheckBox graticule_checkbox;
    boolean graticule = false;
    // Overlay roation
    JSlider rotation_slider;
    int overlay_rotation = 0;
    // Settings button
    JButton settings_button;
    boolean calibrated = false;
    Calibration cal;
    Color[] primary_color = {Color.RED, Color.GREEN, Color.BLUE, Color.CYAN, Color.MAGENTA, Color.YELLOW};
    Color[] secondary_color = {Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.RED, Color.GREEN, Color.BLUE};
    String[] color_options = {"red/cyan", "green/magenta", "blue/yellow", "cyan/red", "magenta/green", "yellow/blue"};
    int color_choice = 1;
    String[] camera_options = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
    int camera_choice = 0;
    int primary_graticule_divisions = 50;
    int secondary_graticule_divisions = 30;
    String[] graticule_orientations = {"vertical", "horizontal"};
    int primary_graticule_orientation = 0;
    int secondary_graticule_orientation = 0;
    
    SystemColor highlight_col;
    
    static LiveWorker liveWorker;
    IplImage ipl_img;
    ImagePanel image_panel;
    
    @Override
    public void run(String string) {
        
        highlight_col = SystemColor.textHighlight;
        
        liveWorker = new LiveWorker();
        
        // Build control panel
        JPanel control_panel = new JPanel();
        control_panel.setLayout( new FlowLayout() );
        // Start/stop button
        live_button = new JButton("Live");
        button_color = live_button.getBackground();
        live_button.setBackground(highlight_col);
        control_panel.add(live_button);
        // Snap button
        snap_button = new JButton("Snap");
        snap_button.setEnabled(false);
        control_panel.add(snap_button);
        // Montage button
        montage_button = new JButton("Montage");
        montage_button.setEnabled(false);
        control_panel.add(montage_button);
        // Stack button
        stack_button = new JButton("Stack");
        stack_button.setEnabled(false);
        control_panel.add(stack_button);
        // Crosshair checkbox
        crosshair_checkbox = new JCheckBox("Crosshair");
        crosshair_checkbox.setSelected(crosshair);
        control_panel.add(crosshair_checkbox);
        // Grid checkbox
        grid_checkbox = new JCheckBox("Grid");
        grid_checkbox.setSelected(grid);
        control_panel.add(grid_checkbox);
        // Graticule checkbox
        graticule_checkbox = new JCheckBox("Graticule");
        graticule_checkbox.setSelected(graticule);
        control_panel.add(graticule_checkbox);
        // Rotation slider
        rotation_slider = new JSlider(-90, +90, 0);
        rotation_slider.setMajorTickSpacing(90);
        rotation_slider.setMinorTickSpacing(5);
        rotation_slider.setPaintTicks(true);
        rotation_slider.setSnapToTicks(true);
        rotation_slider.setPaintLabels(true);
        control_panel.add(rotation_slider);
        // Settings button
        settings_button = new JButton("Settings");
        control_panel.add(settings_button);
        // Add action listeners to buttons
        live_button.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(live) {
                    stopLiveView();
                    //live = false;
                    //liveWorker.stop();
                    if (montaging) {
                        montaging = false;
                        endMontage();
                    }
                    if (stacking) {
                        stacking = false;
                        endStack();
                    }
                    //live_button.setText("Live");
                    //live_button.setBackground(highlight_col);
                    //snap_button.setEnabled(false);
                    //montage_button.setEnabled(false);
                    //stack_button.setEnabled(false);
                } else if (!live) {
                    startLiveView();
                }
            }
        });
        snap_button.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(live) {
                    stopLiveView();
                    //liveWorker.stop();
                    Date date = new Date();
                    Timestamp stamp = new Timestamp( date.getTime() );
                    ImagePlus imp = new ImagePlus( "Snap " + stamp, liveWorker.getSnap() );
                    if (calibrated) {
                        imp.setCalibration(cal);
                    }
                    imp.show();
                    //liveWorker = new LiveWorker();
                    //liveWorker.start();
                }
            }
        });
        montage_button.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!montaging) {
                    liveWorker.stop();
                    GenericDialog gd = new GenericDialog("Configure Montage...");
                    gd.addNumericField("Rows: ", 2, 0);
                    gd.addNumericField("Cols: ", 2, 0);
                    gd.showDialog();
                    if (gd.wasCanceled()) {
                        liveWorker = new LiveWorker();
                        liveWorker.start();
                        return;
                    }
                    montage_rows = (int)gd.getNextNumber();
                    montage_cols = (int)gd.getNextNumber();
                    if ((montage_rows < 1 ) || (montage_cols < 1)) {
                        liveWorker = new LiveWorker();
                        liveWorker.start();
                        return;
                    }
                    montaging = true;
                    image_montage = new Image[montage_rows][montage_cols];
                    snap_button.setEnabled(false);
                    stack_button.setEnabled(false);
                    current_row = 0;
                    current_col = 0;
                    montage_button.setText("row " + (current_row+1) + ", col " + (current_col+1) );
                    montage_button.setBackground(highlight_col);
                    liveWorker = new LiveWorker();
                    liveWorker.start();
                } else {
                    if (current_col < montage_cols) {
                        image_montage[current_row][current_col] = liveWorker.getSnap();
                        current_col++; // increment col
                        montage_button.setBackground(highlight_col);
                        if (current_col < montage_cols) {
                            montage_button.setText("row " + (current_row+1) + ", col " + (current_col+1) );
                        } else {
                            montage_button.setText("row " + (current_row+1+1) + ", col 1");
                        }
                    } else if (current_row < montage_rows) {
                        current_col = 0; // reset column
                        current_row++;   // increment row
                        image_montage[current_row][current_col] = liveWorker.getSnap();
                        current_col++;   // increment row
                        montage_button.setText("row " + (current_row+1) + ", col " + (current_col+1) );
                    }

                    if ((current_row >= montage_rows-1) && (current_col >= montage_cols)) {
                        stopLiveView();
                        //liveWorker.stop();
                        endMontage();
                        //liveWorker = new LiveWorker();
                        //liveWorker.start();
                    }
                }
            }
        });
        stack_button.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!stacking) {
                    liveWorker.stop();
                    GenericDialog gd = new GenericDialog("Configure Stack...");
                    gd.addNumericField("Slices: ", 2, 0);
                    gd.showDialog();
                    if (gd.wasCanceled()) {
                        liveWorker = new LiveWorker();
                        liveWorker.start();
                        return;
                    }
                    stacking = true;
                    snap_button.setEnabled(false);
                    montage_button.setEnabled(false);
                    stack_size = (int)gd.getNextNumber();
                    stack_counter = 0;
                    stack_button.setText("slice " + (stack_counter + 1) );
                    stack_button.setBackground(highlight_col);
                    image_stack = new Image[stack_size];
                    liveWorker = new LiveWorker();
                    liveWorker.start();
                } else {
                    image_stack[stack_counter] = liveWorker.getSnap();
                    stack_counter++;
                    stack_button.setText("slice " + (stack_counter + 1) );
                }
                if (stack_counter +1 > stack_size) {
                    // Next image would exceed specified stack size
                    stopLiveView();
                    //liveWorker.stop();
                    endStack();
                    //liveWorker = new LiveWorker();
                    //liveWorker.start();
                }
            }
        });
        settings_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Pause live view
                if (live) liveWorker.stop();
                // Build settings dialog
                GenericDialog gd = new GenericDialog("Settings...");
                // Camera options
                gd.addMessage("Camera:", new Font("Dialog", Font.BOLD, 12) );
                gd.addChoice("Camera select:", camera_options, camera_options[camera_choice]);
                // Overlay color options
                gd.setInsets(25, 0, 3);
                gd.addMessage("Overlay colours:", new Font("Dialog", Font.BOLD, 12) );
                gd.addChoice("Overlay color (primary/secondary):", color_options, color_options[color_choice]);
                // Graticule options
                gd.setInsets(25, 0, 3);
                gd.addMessage("Graticule options:", new Font("Dialog", Font.BOLD, 12) );
                gd.addNumericField("Graticule divisions (primary):", primary_graticule_divisions, 0);
                gd.addChoice("Graticule orientation (primary):", graticule_orientations, graticule_orientations[primary_graticule_orientation]);
                gd.addNumericField("Graticule divisions (secondary):", secondary_graticule_divisions, 0);
                gd.addChoice("Graticule orientation (secondary):", graticule_orientations, graticule_orientations[secondary_graticule_orientation]);
                // Grid options
                gd.setInsets(25, 0, 3);
                gd.addMessage("Grid options:", new Font("Dialog", Font.BOLD, 12) );
                gd.addNumericField("Grid divisions (horizontal):", grid_divisions_x, 0);
                gd.addNumericField("Grid divisions (vertical):", grid_divisions_y, 0);
                // Calibration
                gd.setInsets(25, 0, 3);
                gd.addMessage("Calibration:", new Font("Dialog", Font.BOLD, 12) );
                if (cal == null) {
                    gd.addNumericField("Pixel width: ", 1, 0);
                    gd.addNumericField("Pixel height: ", 1, 0);
                    gd.addStringField("Units:", "pixels");
                } else {
                    gd.addNumericField("Pixel width: ", cal.pixelWidth, 0);
                    gd.addNumericField("Pixel height: ", cal.pixelHeight, 0);
                    gd.addStringField("Units:", cal.getUnit() );
                }
                gd.showDialog();
                if (gd.wasCanceled()) {
                    if (live) {
                        liveWorker = new LiveWorker();
                        liveWorker.start();
                    }
                    return;
                }
                // Set camera options
                camera_choice = gd.getNextChoiceIndex();
                // Set overlay color options
                color_choice = gd.getNextChoiceIndex();
                // Set graticule options
                primary_graticule_divisions = (int)gd.getNextNumber();
                secondary_graticule_divisions = (int)gd.getNextNumber();
                primary_graticule_orientation = gd.getNextChoiceIndex();
                secondary_graticule_orientation = gd.getNextChoiceIndex();
                // Set grid options
                grid_divisions_x = (int)gd.getNextNumber();
                grid_divisions_y = (int)gd.getNextNumber();
                // Set calibration options
                cal = new Calibration();
                cal.pixelWidth = (double)gd.getNextNumber();
                cal.pixelHeight = (double)gd.getNextNumber();
                cal.setUnit( gd.getNextString() );
                calibrated = true;
                // Re-start live view
                if (live) {
                    liveWorker = new LiveWorker();
                    liveWorker.start();
                }
            }
        });
        // Add listener to checkboxes
        crosshair_checkbox.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                crosshair =! crosshair;
                graticule = false;
                graticule_checkbox.setSelected(false);
                grid = false;
                grid_checkbox.setSelected(false);
                image_panel.repaint();
            }
        });
        graticule_checkbox.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                graticule =! graticule;
                crosshair = false;
                crosshair_checkbox.setSelected(false);
                grid = false;
                grid_checkbox.setSelected(false);
                image_panel.repaint();
            }
        });
        grid_checkbox.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                grid =! grid;
                crosshair = false;
                crosshair_checkbox.setSelected(false);
                graticule = false;
                graticule_checkbox.setSelected(false);
                image_panel.repaint();
            }
        });
        // Add listener to slider
        rotation_slider.addChangeListener( new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                overlay_rotation = rotation_slider.getValue();
                image_panel.repaint();
            }
        });
        // Image panel
        image_panel = new ImagePanel( null );
        // Main window
        jframe = new JFrame(jframe_title);
        jframe.setSize(1024, 780);
        jframe.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                live = false;
                liveWorker.stop();
            }
        });
        jframe.add(control_panel, BorderLayout.PAGE_START);
        jframe.add(image_panel, BorderLayout.CENTER);
        jframe.setVisible(true);
    }
    
    void endMontage() {
        montage_button.setBackground(button_color);
        montaging = false;
        montage_button.setText("Montage");
        snap_button.setEnabled(true);
        stack_button.setEnabled(true);
        // Stitch images by drawing them onto a new Image object of the required size
        BufferedImage stitched_image = new  BufferedImage(montage_cols * liveWorker.getWidth(), montage_rows * liveWorker.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D stitchG = stitched_image.createGraphics();
        int width  = liveWorker.getWidth();
        int height = liveWorker.getHeight();
        for (int row = 0; row < montage_rows; row++) {
            for (int col = 0; col < montage_cols; col++) {
                int x = width * col;
                int y = height * row;
                stitchG.drawImage(image_montage[row][col], x, y, null);
            }
        }
        stitchG.dispose();
        // Use the stiched image to generate an ImagePlus for the montage
        Date date = new Date();
        Timestamp stamp = new Timestamp( date.getTime() );
        ImagePlus imp = new ImagePlus( "Montage " + stamp.toString(), stitched_image );
        if (calibrated) {
            imp.setCalibration(cal);
        }
        imp.show();
    }
    
    void startLiveView() {
        live = true;
        live_button.setText("Stop");
        live_button.setBackground(Color.red);
        snap_button.setEnabled(true);
        montage_button.setEnabled(true);
        stack_button.setEnabled(true);
        live_button.setEnabled(true);
        liveWorker = new LiveWorker();
        liveWorker.start();
    }
    void stopLiveView() {
        live = false;
        liveWorker.stop();
        live_button.setText("Live");
        live_button.setBackground(highlight_col);
        snap_button.setEnabled(false);
        montage_button.setEnabled(false);
        stack_button.setEnabled(false);
    }
    
    void endStack() {
        stacking = false;
        stack_button.setBackground(button_color);
        stack_button.setText("Stack");
        snap_button.setEnabled(true);
        montage_button.setEnabled(true);
        // Count number of non-null slices
        int slice_count;
        for (slice_count = 0; slice_count < stack_size; slice_count++) {
            if (image_stack[slice_count] == null) break;
        }
        if (slice_count == 0) return;
        ImagePlus[] imp = new ImagePlus[slice_count];
        for (int i = 0; i < slice_count; i++) {
            imp[i] = new ImagePlus( "Slice " + i, image_stack[i] );
        }
        ImagePlus stack = new ImagePlus();
        stack.setStack( ImageStack.create(imp) );
        Date date = new Date();
        Timestamp stamp = new Timestamp( date.getTime() );
        stack.setTitle( "Stack " + stamp);
        if (calibrated) {
            stack.setCalibration(cal);
        }
        stack.show();
    }
    
    public class ImagePanel extends JPanel{

        private Image image;
        private Image scaledImage;
        
        public ImagePanel(Image image) {
            this.image = image;
        }
        
        public void setImage(Image image) {
            this.image = image;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g;
            
            int width = this.getWidth();
            int height = this.getHeight();
            int center_x = width/2;
            int center_y = height/2;
            
            if(image == null) return;
            int image_width = this.getWidth();
            int image_height = this.getHeight();
            double grabberAR = liveWorker.getAspectRatio();
            double thisAR = (double) this.getHeight() / (double) this.getWidth();
            if (thisAR > grabberAR) {
                // This ImagePanel is taller than the image
                image_height = (int)( this.getWidth() * grabberAR );
            } else {
                // This ImagePanel is wider than the image
                image_width = (int)( this.getHeight() / grabberAR );
            }
            scaledImage = image.getScaledInstance(image_width, image_height,Image.SCALE_SMOOTH);
            int image_x = center_x - image_width/2;
            int image_y = center_y - image_height/2;
            g2d.drawImage(scaledImage, image_x, image_y, null);
            
            // Draw crosshair
            if (crosshair) {
                g2d.setColor(primary_color[color_choice]);
                g2d.drawLine(center_x, center_y - image_height/2, center_x, center_y + image_height/2);
                g2d.drawLine(center_x - image_width/2, center_y, center_x + image_width/2, center_y);
                g2d.drawOval(center_x  - 50, center_y - 50, 100, 100);
            }
            
            // Draw grid
            if (grid) {
                g2d.setColor(primary_color[color_choice]);
                double grid_spacing_x = (double)image_width / (double)grid_divisions_x;
                double grid_spacing_y = (double)image_height / (double)grid_divisions_y;
                for (int i = 1; i < grid_divisions_y; i++) {
                    // Draw horizontal line
                    g2d.drawLine((int)( (double)center_x - (image_width  / 2.0) ), 
                                 (int)( (double)center_y - (image_height / 2.0) + (i * grid_spacing_y) ),
                                 (int)( (double)center_x + (image_width  / 2.0) ), 
                                 (int)( (double)center_y - (image_height / 2.0) + (i * grid_spacing_y) ) 
                                 );
                }
                for (int i = 1; i < grid_divisions_x; i++) {
                    // Draw vertical line
                    g2d.drawLine((int)( (double)center_x - (image_width  / 2.0) + (i * grid_spacing_x) ),
                                 (int)( (double)center_y - (image_height / 2.0) ), 
                                 (int)( (double)center_x - (image_width  / 2.0) + (i * grid_spacing_x) ), 
                                 (int)( (double)center_y + (image_height / 2.0) ) 
                                 );
                }
            }
            
            // Rotate the graphics object befroe drawing graticule
            g2d.rotate(Math.toRadians(overlay_rotation), center_x, center_y);
            
            // Draw graticule
            if (graticule) {
                int x_offset = 0;
                if (primary_graticule_orientation == secondary_graticule_orientation) x_offset = 10;
                int graticule_length = 10 * image_height / 12;
                int tick_length = 5;
                // Draw the right (secondary) graticule
                if (secondary_graticule_orientation == 1) g2d.rotate(Math.toRadians(+90.0), center_x, center_y);
                g2d.setColor(secondary_color[color_choice]);
                g2d.drawLine(center_x + (+1 * x_offset), center_y - graticule_length/2, center_x + (+1 * x_offset), center_y + graticule_length/2);
                for(int i = 0; i < secondary_graticule_divisions+1; i++) {
                    int tick_factor = 1;
                    if ((i % (secondary_graticule_divisions/6.0) == 0.0) && (secondary_graticule_divisions % 6 == 0)) tick_factor = 2;
                    if ((i % (secondary_graticule_divisions/5.0) == 0.0) && (secondary_graticule_divisions % 5 == 0)) tick_factor = 3;
                    if ((i % (secondary_graticule_divisions/4.0) == 0.0) && (secondary_graticule_divisions % 4 == 0)) tick_factor = 4;
                    if ((i % (secondary_graticule_divisions/3.0) == 0.0) && (secondary_graticule_divisions % 3 == 0)) tick_factor = 5;
                    if ((i % (secondary_graticule_divisions/2.0) == 0.0) && (secondary_graticule_divisions % 2 == 0)) tick_factor = 6;
                    int current_y = (int) ( ( (double)center_y - (double)graticule_length/2.0) + (double)i*( (double)graticule_length/secondary_graticule_divisions ) );
                    g2d.drawLine(center_x + (+1 * x_offset), current_y, center_x + (+1 * x_offset) + (tick_length*tick_factor), current_y );
                }
                if (secondary_graticule_orientation == 1) g2d.rotate(Math.toRadians(-90.0), center_x, center_y);
                
                // Draw the left (primary) graticule
                if (primary_graticule_orientation == 1) g2d.rotate(Math.toRadians(+90.0), center_x, center_y);
                g2d.setColor(primary_color[color_choice]);
                g2d.drawLine(center_x + (-1 * x_offset), center_y - graticule_length/2, center_x + (-1 * x_offset), center_y + graticule_length/2);
                for(int i = 0; i < primary_graticule_divisions+1; i++) {
                    int tick_factor = 1;
                    if ((i % (primary_graticule_divisions/6.0) == 0.0) && (primary_graticule_divisions % 6 == 0)) tick_factor = 2;
                    if ((i % (primary_graticule_divisions/5.0) == 0.0) && (primary_graticule_divisions % 5 == 0)) tick_factor = 3;
                    if ((i % (primary_graticule_divisions/4.0) == 0.0) && (primary_graticule_divisions % 4 == 0)) tick_factor = 4;
                    if ((i % (primary_graticule_divisions/3.0) == 0.0) && (primary_graticule_divisions % 3 == 0)) tick_factor = 5;
                    if ((i % (primary_graticule_divisions/2.0) == 0.0) && (primary_graticule_divisions % 2 == 0)) tick_factor = 6;
                    int current_y = (int) ( ( (double)center_y - (double)graticule_length/2.0) + (double)i*( (double)graticule_length/primary_graticule_divisions ) );
                    g2d.drawLine(center_x + (-1 * x_offset), current_y, center_x + (-1 * x_offset) - (tick_length*tick_factor), current_y );
                }
                if (primary_graticule_orientation == 1) g2d.rotate(Math.toRadians(-90.0), center_x, center_y);
            }
            
            // Rotate the graphics object back again
            g2d.rotate(Math.toRadians(-overlay_rotation), center_x, center_y);
        }
    }
    
    public static Image toImage(IplImage src) {
        OpenCVFrameConverter.ToIplImage iplConverter = new OpenCVFrameConverter.ToIplImage();
        Java2DFrameConverter bimConverter = new Java2DFrameConverter();
        Frame frame = iplConverter.convert(src);
        BufferedImage img = bimConverter. convert(frame);
        Image result = img.getScaledInstance(
          img.getWidth(), img.getHeight(), java.awt.Image.SCALE_DEFAULT);
        img.flush();
        return result;
    }
    
    public class LiveWorker implements Runnable {
        
        Thread worker;
        Boolean running = false;
        Image snap;
        double width;
        double height;
        double aspect_ratio;
        int sleepTime = 50;
        OpenCVFrameGrabber grabber;
        //VideoInputFrameGrabber grabber;
        
        public LiveWorker() {
            
            //IJ.log("Creating OpenCVFrameGrabber...");
            grabber = new OpenCVFrameGrabber(camera_choice);
            //IJ.log("Starting grabber...");
            try {
                grabber.start();
            } catch (FrameGrabber.Exception ex) {
                Logger.getLogger(Webcam_J.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        public void start() {
            running = true;
            width = grabber.getImageWidth();
            height = grabber.getImageHeight();
            aspect_ratio = height / width;
            jframe.setTitle(jframe_title + " - Camera " + camera_options[camera_choice] + " (" + (int)width + " x " + (int)height + ")");
            worker = new Thread(this);
            worker.start();
        }
        
        public void stop() {
            running = false;
        }
        
        public Image getSnap() {
            return snap;
        }
        
        public double getAspectRatio() {
            return aspect_ratio;
        }
        
        public int getWidth() {
            return (int) width;
        }
        
        public int getHeight() {
            return (int) height;
        }

        @Override
        public void run() {
            Frame frame;
            while(running) {
                try {
                    frame = grabber.grab();
                    OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
                    ipl_img = converter.convert(frame);
                    snap = toImage(ipl_img);
                    image_panel.setImage( snap );
                    image_panel.repaint();
                    sleep(sleepTime);
                } catch (FrameGrabber.Exception ex) {
                    Logger.getLogger(Webcam_J.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Webcam_J.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
    }
}
