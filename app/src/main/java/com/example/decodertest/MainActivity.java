package com.example.decodertest;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
//import android.

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static boolean VERBOSE_MAIN = true;
    private static int MICRO_SEC = 1000000;
    private static String TAG = "Debugg";

    private int mWidth = -1;
    private int mHeight = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void Test(View view) {
        DecoderWrapper decoderWrapper = new DecoderWrapper();
        Thread decThread = new Thread(decoderWrapper);
        decThread.start();
        try {
            decThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private class DecoderWrapper implements Runnable {

        ArrayList<ByteBuffer> FrameData;
        ArrayList<Long> FramePTS;
        ArrayList<Integer> FrameSize;
        ArrayList<Integer> FrameFlags;

        private boolean VERBOSE_DECODER = true;
        private String MIME_TYPE = "video/hevc";

        private Throwable mThrowable;


        @Override
        public void run() {
            getFramesStore();
            setParameters(256, 180);
            decodeData();

        }

        private void getFramesStore() {

            boolean VERBOSE_GET_FRAME_DATA = true; //Debug related

            FrameData = new ArrayList<ByteBuffer>();
            FramePTS = new ArrayList<Long>();
            FrameSize = new ArrayList<Integer>();
            FrameFlags = new ArrayList<Integer>();

            int count = 0;

            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.frame_053_0);

            MediaExtractor tempExtractor = new MediaExtractor();

            //tempExtractor.setDataSource(this, videoUri, null);
            try {
                tempExtractor.setDataSource(getApplicationContext(), videoUri, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int nTracks = tempExtractor.getTrackCount();

            // Begin by unselecting all of the tracks in the extractor, so we won't see
            // any tracks that we haven't explicitly selected.
            for (int i = 0; i < nTracks; ++i) {
                tempExtractor.unselectTrack(i);
            }


            /*Create the mCodecWrapper only once*/
            for (int i = 0; i < nTracks; ++i) {
                tempExtractor.selectTrack(i);
            }

            int size;
            int counter = 0;
            /**Add buffer to the arraylist then start filling the buffer*/
            FrameData.add(ByteBuffer.allocate(512));
            size = tempExtractor.readSampleData(FrameData.get(counter), 0);
            counter++;

            while (size > 0) {

                /**Adding frame related data to the buffer*/
                FramePTS.add(MICRO_SEC * count + tempExtractor.getSampleTime());
                FrameSize.add(size);
                FrameFlags.add(tempExtractor.getSampleFlags());

                if (VERBOSE_MAIN)
                    if (VERBOSE_DECODER)
                        if (VERBOSE_GET_FRAME_DATA)
                            Log.d(TAG, "Size: " + size + " Pts: " + FramePTS.get(counter - 1) + " Flags: " + FrameFlags.get(counter - 1));


                tempExtractor.advance();
                FrameData.add(ByteBuffer.allocate(512));
                size = tempExtractor.readSampleData(FrameData.get(counter), 0);
                counter++;

            }
            tempExtractor.release();
        }


        private void decodeData() {
            boolean VERBOSE_DECODE_FRAMES = true;
            OutputSurface outputSurface = null;
            MediaCodec decoder = null;
            MediaFormat mediaFormat;


            MediaExtractor extractor = new MediaExtractor();
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.frame_055_0);
            try {
                extractor.setDataSource(getApplicationContext(), videoUri, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            MediaFormat format = extractor.getTrackFormat(0);
            String mime = format.getString(MediaFormat.KEY_MIME);

            extractor.selectTrack(0);


            if (VERBOSE_MAIN)
                if (VERBOSE_DECODER)
                    if (VERBOSE_DECODE_FRAMES)
                        Log.d(TAG, "editVideoFile " + mWidth + " x" + mHeight + " Decoder code");

            try {
                decoder = MediaCodec.createDecoderByType(MIME_TYPE);

                outputSurface = new OutputSurface(mWidth, mHeight);

                if (VERBOSE_MAIN) if (VERBOSE_DECODER) if (VERBOSE_DECODE_FRAMES)
                    Log.d(TAG, "editVideoFile " + mWidth + " x" + mHeight + outputSurface.toString());

                mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, 256, 180);

                byte[] bytes = new byte[]{(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01};
                ByteBuffer bb = ByteBuffer.wrap(bytes);
                mediaFormat.setByteBuffer("csd-0", bb);

                //mediaFormat.setByteBuffer("csd-0",java.nio.HeapByteBuffer[pos=0 lim=2157 cap=2157]);
                //mediaFormat.setByteBuffer("csd-0",java.nio.HeapByteBuffer[pos=0,lim=2157,cap=2157];
                //mediaFormat.setInteger("display-width",format.getInteger("display-width"));
                //mediaFormat.setInteger("display-height",format.getInteger("display-height"));

                //mediaFormat.setInteger("width",format.getInteger("width"));
                //mediaFormat.setInteger("width",format.getInteger("width"));

                /*if (VERBOSE_MAIN)
                    if (VERBOSE_DECODER)
                        if (VERBOSE_DECODE_FRAMES) {
                            //Set<String> featureSet = format.getFeatures();

                            Log.d(TAG, "Format details :" +  format.getNumber(format.KEY_MIME)+ " bitrate");
                            Log.d(TAG, "Format details :" +  format.getNumber(format.KEY_COLOR_FORMAT) + "Color Format");
                            Log.d(TAG, "Format details :" +  format.getNumber(format.KEY_MAX_WIDTH)+ " Width");
                            Log.d(TAG, "Format details :" +  format.getNumber(format.KEY_MAX_HEIGHT )+ " Hieght");
                            Log.d(TAG, "Format details :" +  format.getNumber(format.KEY_I_FRAME_INTERVAL) + "I Frame Interval");


                        }*/

                decoder.configure(format, outputSurface.getSurface(), null, 0);

                decoder.start();

                getVideoFrames(decoder, outputSurface);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (outputSurface != null) {
                    outputSurface.release();
                }
                if (decoder != null) {
                    decoder.stop();
                    decoder.release();
                }
            }
        }

        private void setParameters(int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        private void getVideoFrames(MediaCodec decoder, OutputSurface outputSurface) {

            boolean VERBOSE_GET_VIDEOFRAMES = true;

            if (VERBOSE_MAIN)
                if (VERBOSE_DECODER)
                    if (VERBOSE_GET_VIDEOFRAMES)
                        Log.d(TAG, "Get video frame function");

            final int TIMEOUT_USEC = 10000;

            ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
            ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int inputChunk = 0;
            int outputCount = 0;

            boolean outputDone = false;
            boolean inputDone = false;
            boolean decoderDone = false;

            while (!outputDone) {

                if (VERBOSE_MAIN)
                    if (VERBOSE_DECODER)
                        if (VERBOSE_GET_VIDEOFRAMES)
                            Log.d(TAG, "Loop funciton for getting frames");


                if (!inputDone) {
                    /**get index for the next input buffer data*/
                    int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);

                    if (inputBufIndex >= 0) {
                        if (inputChunk == FrameSize.size()) {

                            if (VERBOSE_MAIN)
                                if (VERBOSE_DECODER)
                                    if (VERBOSE_GET_VIDEOFRAMES)
                                        Log.d(TAG, "Adding EOS");

                            decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;

                            if (VERBOSE_MAIN)
                                if (VERBOSE_DECODER)
                                    if (VERBOSE_GET_VIDEOFRAMES)
                                        Log.d(TAG, "Input done: " + inputDone);
                        } else {
                            ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                            //inputBuf.clear();

                            /**Copying data to the decoder inputBuf*/
                            int tempSize = FrameSize.get(inputChunk);
                            byte[] tempByte = new byte[tempSize];
                            FrameData.get(inputChunk).get(tempByte, 0, tempSize);
                            /*for(int i =0;i<tempSize;i++){
                                if (VERBOSE_MAIN) if (VERBOSE_DECODER) if (VERBOSE_GET_VIDEOFRAMES)
                                            Log.d(TAG,  String.valueOf(tempByte[i]));
                            }*/
                            /*if (VERBOSE_MAIN) if (VERBOSE_DECODER) if (VERBOSE_GET_VIDEOFRAMES)
                                Log.d(TAG,  "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
                            if (VERBOSE_MAIN) if (VERBOSE_DECODER) if (VERBOSE_GET_VIDEOFRAMES)
                                Log.d(TAG,  "");*/
                            inputBuf.put(tempByte);

                            int flags = FrameFlags.get(inputChunk);
                            Long time = FramePTS.get(inputChunk);

                            if (inputChunk == 0)
                                decoder.queueInputBuffer(inputBufIndex, 0, tempSize, time, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                            else
                                decoder.queueInputBuffer(inputBufIndex, 0, tempSize, time, flags);

                            if (VERBOSE_MAIN)
                                if (VERBOSE_DECODER)
                                    if (VERBOSE_GET_VIDEOFRAMES)
                                        Log.d(TAG, "Submitted Frame: " + inputChunk + " " + tempSize);
                            inputChunk++;
                        }
                    } else {
                        if (VERBOSE_MAIN)
                            if (VERBOSE_DECODER)
                                if (VERBOSE_GET_VIDEOFRAMES)
                                    Log.d(TAG, "Input buffer is not available");
                    }
                }

                boolean decoderOutputAvailable = !decoderDone;

                if (!outputDone) {

                    int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);

                    if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        if (VERBOSE_MAIN)
                            if (VERBOSE_DECODER)
                                if (VERBOSE_GET_VIDEOFRAMES)
                                    Log.d(TAG, "no output from decoder available " + inputChunk);

                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        if (VERBOSE_MAIN)
                            if (VERBOSE_DECODER)
                                if (VERBOSE_GET_VIDEOFRAMES)
                                    Log.d(TAG, "Decoder output buffer changed " + inputChunk);
                        outputBuffers = decoder.getOutputBuffers();

                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat newFormat = decoder.getOutputFormat();
                        if (VERBOSE_MAIN)
                            if (VERBOSE_DECODER)
                                if (VERBOSE_GET_VIDEOFRAMES)
                                    Log.d(TAG, "decoder output format changed: " + newFormat + inputChunk);
                    } else if (decoderStatus < 0) {
                        if (VERBOSE_MAIN)
                            if (VERBOSE_DECODER)
                                if (VERBOSE_GET_VIDEOFRAMES)
                                    Log.d(TAG, "decoder  Failed: " + inputChunk + " " + decoderStatus);

                    } else {
                        if (VERBOSE_MAIN)
                            if (VERBOSE_DECODER)
                                if (VERBOSE_GET_VIDEOFRAMES)
                                    Log.d(TAG, "Decoder have given a buffer: " + decoderStatus + " (size=" + info.size + ")");

                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            if (VERBOSE_MAIN)
                                if (VERBOSE_DECODER)
                                    if (VERBOSE_GET_VIDEOFRAMES)
                                        Log.d(TAG, "output EOS" + " " + decoderStatus);
                            outputDone = true;
                        }
                        ByteBuffer buffer = outputBuffers[decoderStatus];

                        boolean doRender = (info.size != 0);
                        decoder.releaseOutputBuffer(decoderStatus, doRender);
                        /*if (doRender) {
                            // This waits for the image and renders it after it arrives.
                            if (VERBOSE_MAIN)
                                if (VERBOSE_DECODER)
                                    if (VERBOSE_GET_VIDEOFRAMES)
                                        Log.d(TAG, "awaiting frame");

                            outputSurface.awaitNewImage();
                            outputSurface.drawImage();
                            // Send it to the encoder.

                            if (VERBOSE_MAIN)
                                if (VERBOSE_DECODER)
                                    if (VERBOSE_GET_VIDEOFRAMES)
                                        Log.d(TAG, "swapBuffers");
                        }*/
                    }
                }
            }
        }


        private  ByteBuffer extractHevcParamSets(byte[] bitstream) {
            final byte[] startCode = {0x00, 0x00, 0x00, 0x01};
            int nalBeginPos = 0, nalEndPos = 0;
            int nalUnitType = -1;
            int nlz = 0;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (int pos = 0; pos < bitstream.length; pos++) {
                if (2 <= nlz && bitstream[pos] == 0x01) {
                    nalEndPos = pos - nlz;
                    if (nalUnitType == 32 || nalUnitType == 33 || nalUnitType == 34) {
                        // extract VPS(32), SPS(33), PPS(34)
                        Log.d(TAG, "NUT=" + nalUnitType + " range={" + nalBeginPos + "," + nalEndPos + "}");
                        try {
                            baos.write(startCode);
                            baos.write(bitstream, nalBeginPos, nalEndPos - nalBeginPos);
                        } catch (IOException ex) {
                            Log.e(TAG, "extractHevcParamSets", ex);
                            return null;
                        }
                    }
                    nalBeginPos = ++pos;
                    nalUnitType = (bitstream[pos] >> 1) & 0x2f;
                    if (0 <= nalUnitType && nalUnitType <= 31) {
                        break;  // VCL NAL; no more VPS/SPS/PPS
                    }
                }
                nlz = (bitstream[pos] != 0x00) ? 0 : nlz + 1;
            }
            return ByteBuffer.wrap(baos.toByteArray());
        }

    }
}
