<header>
# Blog Redesign

<time class="article-date" date="2019-09-13">2019-09-13</time>
</header>

<abstract>
  This is a mini project summary for my blog redesign.
  It covers design decisions and examples, mostly on the topic of typography.
  The source blog was build on an out of the box octopres design, which started to 
  look a bit dated to me.
</abstract>


## The goal

Surveying the posts I had written, I realised that most post are comprised of only text.
The only pictures, where like stock photos. They where aimed to impress, and where not required 
to understand the topic of the post.

I therefore decided to focus on typography. If the text itself is beautiful, then I would not need
to add stock photos.
Also I wanted to come up with a very simple clean design, which loads fast and could stand the test
of time due to its simplicity.

## Mobile first

Personally I'm reading blog posts mostly on my mobile phone and modern web design 
always starts with creating a good mobile experience, which can then be augmented for larger screens.

The goal here was to have large readable text with no distractions.

<img alt='Mobile blog post screenshot' width='411px' src='/images/blog/2019-09-13-blog-redesign/mobile-post-visible.png' />

<div>
The design is a one column of text, with extra line spacing and 
a font with large [x-height](https://en.wikipedia.org/wiki/X-height) in
this case [Open Sans](https://fonts.google.com/specimen/Open+Sans?selection.family=Open+Sans).
</div>
Also the width of the page content is constrained to `35em` as a relative measure of text, in
an effort to make sure that lines of text remain short enough the be read comfortably.

```css
.body {
  font-family: 'Open Sans', 'Quattrocento Sans', sans-serif;
  color: #111;
  background-color: #FFFFFF;
  font-size: 100%;
}

#page {
  margin: 20px auto;
  padding: 0px 1em;
  max-width: 35em;
  line-height: 170%;
}
```

## About typography

- link to 3 intro videos
  - [Introduction to Typography: Talking Type](https://www.youtube.com/watch?v=A80N9HNs_u0&list=PLc2D1VhlKiew26m_Zqu2mS0LJoxOt5oXK&index=2&t=0s)
  - [Introduction to Typography: Typefaces and their Stories](https://www.youtube.com/watch?v=pv5xYjvhz_k&list=PLc2D1VhlKiew26m_Zqu2mS0LJoxOt5oXK&index=2)
  - [Introduction to Typography: Putting Type to Work](https://www.youtube.com/watch?v=cBT9aCTKbww&list=PLc2D1VhlKiew26m_Zqu2mS0LJoxOt5oXK&index=3)
  - [Richard Rutter | Web Typography | CSS Day 2018](https://www.youtube.com/watch?v=hbIZX6tE9JY&t=1983s)
- front super families
- explain font choices
 - Quattrocento (header)
 - Quattrocento sans (body then replaced with Open Sanse due to better x height)
- extra line height
- bottom margin on paragraphs
- max with for page content to ensure line length is not too much

## Extras for the desktop

- left side bar, with nav for all the posts
- margin on the right exracting all the links
 - potential for further applications

## Summary
