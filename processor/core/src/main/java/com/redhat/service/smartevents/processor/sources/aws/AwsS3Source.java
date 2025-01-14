package com.redhat.service.smartevents.processor.sources.aws;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface AwsS3Source extends GatewayBean {

    String TYPE = "AwsS3";

    String BUCKET_NAME_OR_ARN_PARAMETER = "aws_bucket_name_or_arn";
    String REGION_PARAMETER = "aws_region";
    String ACCESS_KEY_PARAMETER = "aws_access_key";
    String SECRET_KEY_PARAMETER = "aws_secret_key";
    String IGNORE_BODY_PARAMETER = "aws_ignore_body";
    String DELETE_AFTER_READ_PARAMETER = "aws_delete_after_read";
    String PREFIX = "aws_prefix";

    @Override
    default String getType() {
        return TYPE;
    }
}
