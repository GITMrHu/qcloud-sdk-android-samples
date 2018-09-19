package com.tencent.qcloud.csp.sample;

/**
 * Created by rickenwang on 2018/9/18.
 * <p>
 * Copyright (c) 2010-2020 Tencent Cloud. All rights reserved.
 */
import android.content.Context;
import android.support.annotation.Nullable;

import com.tencent.cos.xml.CosXmlService;
import com.tencent.cos.xml.CosXmlServiceConfig;
import com.tencent.cos.xml.exception.CosXmlClientException;
import com.tencent.cos.xml.exception.CosXmlServiceException;
import com.tencent.cos.xml.listener.CosXmlProgressListener;
import com.tencent.cos.xml.model.CosXmlRequest;
import com.tencent.cos.xml.model.bucket.PutBucketRequest;
import com.tencent.cos.xml.model.bucket.PutBucketResult;
import com.tencent.cos.xml.model.object.PutObjectRequest;
import com.tencent.cos.xml.model.object.PutObjectResult;
import com.tencent.cos.xml.model.service.GetServiceRequest;
import com.tencent.cos.xml.model.service.GetServiceResult;
import com.tencent.cos.xml.transfer.UploadService;
import com.tencent.qcloud.core.auth.COSXmlSignSourceProvider;
import com.tencent.qcloud.core.auth.COSXmlSigner;
import com.tencent.qcloud.core.auth.QCloudCredentialProvider;
import com.tencent.qcloud.core.auth.QCloudCredentials;
import com.tencent.qcloud.core.auth.QCloudSignSourceProvider;
import com.tencent.qcloud.core.auth.QCloudSigner;
import com.tencent.qcloud.core.auth.ShortTimeCredentialProvider;
import com.tencent.qcloud.core.common.QCloudClientException;
import com.tencent.qcloud.core.http.HttpConstants;
import com.tencent.qcloud.core.http.QCloudHttpRequest;
import com.tencent.qcloud.core.http.RequestBodySerializer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

/**
 *
 * Created by rickenwang on 2018/6/29.
 * <p>
 * Copyright (c) 2010-2017 Tencent Cloud. All rights reserved.
 */
public class RemoteStorage {

    private int MULTIPART_UPLOAD_SIZE = 1024 * 2;

    private CosXmlService cosXmlService;
    private boolean isHttps;
    private String appid;
    private String region;


    public RemoteStorage(Context context, String appid, String region, String domainSuffix) {

        isHttps = false;
        this.appid = appid;
        this.region = region;

        /**
         * 初始化配置
         */
        CosXmlServiceConfig cosXmlServiceConfig = new CosXmlServiceConfig.Builder()
                .isHttps(isHttps)
                .setAppidAndRegion(appid, region) // appid 和 region 均可以为空
                .setDebuggable(true)
                .setDomainSuffix(domainSuffix)  // 私有云需要设置主域名
                .builder();

        /**
         * 私有云暂时不支持临时密钥进行签名，如果直接在客户端直接使用永久密钥会有安全性问题，因此这里采用
         * 服务端直接下发签名的方式来进行鉴权。
         */
        QCloudSigner qCloudSigner = new MyQCloudSigner();

        cosXmlService = new CosXmlService(context, cosXmlServiceConfig, qCloudSigner);
    }


    /**
     * 列出所有的 bucket
     */
    public GetServiceResult getService() throws CosXmlServiceException, CosXmlClientException {

        GetServiceRequest getServiceRequest = new GetServiceRequest();

        return cosXmlService.getService(getServiceRequest);
    }


    /**
     * 创建 bucket
     *
     * @param bucketName bucket 名称
     */
    public PutBucketResult putBucket(String bucketName) throws CosXmlServiceException, CosXmlClientException {

        PutBucketRequest putBucketRequest = new PutBucketRequest(bucketName);

        return cosXmlService.putBucket(putBucketRequest);
    }

    /**
     * 上传文件
     *
     * @param bucketName bucket 名称
     * @param cosPath 上传到 COS 的路径
     * @param localPath 需要上传文件的本地路径
     * @param progressListener  进度监听器
     *
     * @return 本次上传的 id，可以通过这个 id 来取消上传
     */
    public UploadService.UploadServiceResult uploadFile(String bucketName, String cosPath, String localPath, CosXmlProgressListener progressListener)
            throws CosXmlServiceException, CosXmlClientException {

        UploadService.ResumeData resumeData = new UploadService.ResumeData();
        resumeData.sliceSize = MULTIPART_UPLOAD_SIZE; // 分片上传的大小
        resumeData.cosPath = cosPath;
        resumeData.bucket = bucketName;
        resumeData.srcPath = localPath;

        /**
         * 上传服务类，这个类封装了 {@link CosXmlService} 几个上传相关的接口，通过使用该接口，您可以更加方便的上传文件。
         * 注意，每次上传都要初始化一个新的 {@link CosXmlService} 对象。
         */
        final UploadService uploadService = new UploadService(cosXmlService, resumeData);
        uploadService.setProgressListener(progressListener);
        return uploadService.upload();
    }

    public PutObjectResult simpleUploadFile(String bucketName, String cosPath, String localPath, CosXmlProgressListener progressListener)
            throws CosXmlServiceException, CosXmlClientException {

        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, cosPath, localPath);
        putObjectRequest.setProgressListener(progressListener);

        return cosXmlService.putObject(putObjectRequest);
    }

}
