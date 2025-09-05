import boto3
from botocore.exceptions import ClientError
from botocore.client import Config
from app.core.config import settings
import uuid
import os
from datetime import datetime, timedelta
from typing import Optional


class S3Service:
    def __init__(self):
        self.s3_client = boto3.client(
            's3',
            region_name=os.getenv("AWS_REGION"),
            aws_access_key_id=os.getenv("AWS_ACCESS_KEY_ID"),
            aws_secret_access_key=os.getenv("AWS_SECRET_ACCESS_KEY"),
            config=Config(signature_version="s3v4")
        )
        self.bucket_name = settings.s3_bucket_name
    
    def create_presigned_url(self, bucket: str, key: str, expiration=3600):
        """
        Create a presigned URL for uploading to S3
        """
        return self.s3_client.generate_presigned_url(
            "put_object",
            Params={"Bucket": bucket, "Key": key},
            ExpiresIn=expiration
        )
    
    def generate_presigned_upload_url(
        self, 
        file_name: str, 
        mime_type: str, 
        event_id: int,
        expires_in: int = 3600
    ) -> dict:
        """
        Generate a pre-signed URL for uploading a file to S3
        """
        try:
            # Generate unique S3 key
            file_extension = file_name.split('.')[-1] if '.' in file_name else ''
            unique_filename = f"{uuid.uuid4()}.{file_extension}"
            s3_key = f"events/{event_id}/{unique_filename}"
            
            # Generate pre-signed URL
            presigned_url = self.s3_client.generate_presigned_url(
                'put_object',
                Params={
                    'Bucket': self.bucket_name,
                    'Key': s3_key,
                    'ContentType': mime_type
                },
                ExpiresIn=expires_in
            )
            
            return {
                'upload_url': presigned_url,
                's3_key': s3_key,
                'expires_in': expires_in
            }
            
        except ClientError as e:
            raise Exception(f"Error generating pre-signed URL: {str(e)}")
    
    def generate_presigned_download_url(
        self, 
        s3_key: str, 
        expires_in: int = 3600
    ) -> str:
        """
        Generate a pre-signed URL for downloading a file from S3
        """
        try:
            presigned_url = self.s3_client.generate_presigned_url(
                'get_object',
                Params={
                    'Bucket': self.bucket_name,
                    'Key': s3_key
                },
                ExpiresIn=expires_in
            )
            return presigned_url
            
        except ClientError as e:
            raise Exception(f"Error generating download URL: {str(e)}")
    
    def delete_object(self, s3_key: str) -> bool:
        """
        Delete an object from S3
        """
        try:
            self.s3_client.delete_object(
                Bucket=self.bucket_name,
                Key=s3_key
            )
            return True
        except ClientError as e:
            print(f"Error deleting object {s3_key}: {str(e)}")
            return False
    
    def get_object_url(self, s3_key: str) -> str:
        """
        Get the public URL for an S3 object
        """
        return f"https://{self.bucket_name}.s3.{settings.aws_region}.amazonaws.com/{s3_key}"
    
    def check_object_exists(self, s3_key: str) -> bool:
        """
        Check if an object exists in S3
        """
        try:
            self.s3_client.head_object(Bucket=self.bucket_name, Key=s3_key)
            return True
        except ClientError:
            return False


# Create global instance
s3_service = S3Service()

# Standalone function for direct use
def create_presigned_url(bucket: str, key: str, expiration=3600):
    """
    Standalone function to create presigned URL
    """
    s3_client = boto3.client(
        "s3",
        region_name=os.getenv("AWS_REGION"),
        aws_access_key_id=os.getenv("AWS_ACCESS_KEY_ID"),
        aws_secret_access_key=os.getenv("AWS_SECRET_ACCESS_KEY"),
        config=Config(signature_version="s3v4")
    )
    return s3_client.generate_presigned_url(
        "put_object",
        Params={"Bucket": bucket, "Key": key},
        ExpiresIn=expiration
    )
