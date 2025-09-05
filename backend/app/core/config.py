from pydantic_settings import BaseSettings
from typing import List
import os


class Settings(BaseSettings):
    # Database Configuration
    database_url: str = "postgresql://username:password@localhost:5432/kiddozz_demo"
    db_host: str = "localhost"
    db_port: int = 5432
    db_name: str = "kiddozz_demo"
    db_user: str = "username"
    db_password: str = "password"
    
    # AWS S3 Configuration
    aws_access_key_id: str = ""
    aws_secret_access_key: str = ""
    aws_region: str = "us-east-1"
    s3_bucket_name: str = "kiddozz-images"
    
    # Application Configuration
    secret_key: str = "your-secret-key-here"
    algorithm: str = "HS256"
    access_token_expire_minutes: int = 30
    
    # CORS Configuration
    allowed_origins: List[str] = ["http://localhost:3000", "http://localhost:8080"]
    
    # API Configuration
    api_v1_str: str = "/api/v1"
    project_name: str = "Kiddozz API"
    
    class Config:
        env_file = ".env"
        case_sensitive = False


settings = Settings()
