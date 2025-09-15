# Makefile for backend

run:
	poetry run uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

test:
	pytest -vv --tb=long --disable-warnings

shell:
	poetry shell

lock:
	poetry lock
	poetry install

migrate:
	poetry run alembic upgrade head

makemigration:
	poetry run alembic revision --autogenerate -m "$(name)"

format:
	poetry run black app tests

lint:
	poetry run ruff check app tests

fix:
	poetry run black app tests
	poetry run ruff check --fix app tests
