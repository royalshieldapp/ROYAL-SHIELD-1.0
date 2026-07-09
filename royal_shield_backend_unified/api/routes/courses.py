"""
Cybersecurity Courses API Routes
Ported from Node.js backend - educational content catalog
"""
from fastapi import APIRouter
from pydantic import BaseModel
from typing import List, Optional
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/courses", tags=["Education"])


class CourseModel(BaseModel):
    id: str
    title: str
    description: str
    duration: str
    level: str
    videoUrl: Optional[str] = ""
    thumbnailUrl: Optional[str] = ""


class CoursesResponse(BaseModel):
    courses: List[CourseModel]


# Course catalog — in production, load from database
COURSES_CATALOG = [
    CourseModel(
        id="c1",
        title="Cybersecurity Basics",
        description="Protect yourself from online threats. Learn fundamental concepts of digital security.",
        duration="45 min",
        level="Beginner"
    ),
    CourseModel(
        id="c2",
        title="Phishing Awareness",
        description="How to spot fake emails, SMS, and social engineering attacks.",
        duration="30 min",
        level="Intermediate"
    ),
    CourseModel(
        id="c3",
        title="Network Security",
        description="Securing your home Wi-Fi and understanding network vulnerabilities.",
        duration="60 min",
        level="Advanced"
    ),
    CourseModel(
        id="c4",
        title="Password Management",
        description="Best practices for creating and managing secure passwords.",
        duration="20 min",
        level="Beginner"
    ),
    CourseModel(
        id="c5",
        title="Mobile Device Security",
        description="Protecting your smartphone from malware, spyware, and unauthorized access.",
        duration="35 min",
        level="Intermediate"
    ),
]


@router.get("/", response_model=CoursesResponse)
async def get_courses():
    """
    Get the cybersecurity courses catalog.

    Returns available courses with their metadata.
    """
    return CoursesResponse(courses=COURSES_CATALOG)
