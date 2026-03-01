terraform {
  backend "s3" {
    bucket = "framedrop-infra-2"
    key    = "terraform/ecs/video-processing/state.tfstate"
    region = "us-east-1"
  }
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "6.21.0"
    }
  }
}

provider "aws" {
  region = "us-east-1"
}
