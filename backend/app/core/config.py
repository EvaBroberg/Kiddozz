import os
from typing import List, Union

from dotenv import load_dotenv
from pydantic import field_validator
from pydantic_settings import BaseSettings

# Load environment variables from .env file
load_dotenv()


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
    aws_bucket_name: str = "kiddozz-images"
    s3_bucket_name: str = "kiddozz-images"

    # JWT Configuration
    secret_key: str = "your-secret-key-here-change-in-production"
    algorithm: str = "HS256"
    access_token_expire_minutes: int = 30

    # Environment Configuration
    environment: str = os.getenv("ENVIRONMENT", "development")

    # CORS Configuration
    allowed_origins: Union[List[str], str] = [
        "http://localhost:3000",
        "http://localhost:8080",
    ]

    @field_validator("allowed_origins", mode="before")
    @classmethod
    def parse_allowed_origins(cls, v):
        if isinstance(v, str):
            return [origin.strip() for origin in v.split(",")]
        return v

    # API Configuration
    api_v1_str: str = "/api/v1"
    project_name: str = "Kiddozz API"

    class Config:
        env_file = ".env"
        case_sensitive = False
        env_parse_none_str = "None"


settings = Settings()
