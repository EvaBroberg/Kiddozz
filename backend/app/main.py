import os

from fastapi import FastAPI

from app.api import events, health

app = FastAPI(title="Kiddozz Backend API", version="1.0.0")

# Register routers
app.include_router(health.router)
app.include_router(events.router, prefix="/api/v1/events")


@app.get("/")
def read_root():
    return {"message": "Welcome to Kiddozz API", "version": "1.0.0", "docs": "/docs"}


if __name__ == "__main__":
    import uvicorn

    port = int(os.environ.get("PORT", 8000))
    uvicorn.run("app.main:app", host="0.0.0.0", port=port, reload=True)
