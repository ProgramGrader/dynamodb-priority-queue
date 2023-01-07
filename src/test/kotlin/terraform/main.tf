
provider "aws" {
  alias  = "primary"
  region = var.primary_aws_region
}

resource "aws_dynamodb_table" "assignment_schedule" {

  name     = "assignment_schedule"
  billing_mode = "PAY_PER_REQUEST"
  hash_key = "id"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "scheduled"
    type = "S"
  }


  attribute {
    name = "last_updated_timestamp"
    type = "S"
  }

  attribute {
    name = "queued"
    type = "N"
  }

  attribute {
    name = "DLQ"
    type = "N"
  }

  global_secondary_index {
    name               = "scheduled-index"
    hash_key           = "queued"
    range_key          = "scheduled"
    projection_type    = "ALL"
  }

  global_secondary_index {
    name               = "dlq-last_updated_timestamp-index"
    hash_key           = "DLQ"
    range_key          = "last_updated_timestamp"
    projection_type    = "ALL"
  }


}

